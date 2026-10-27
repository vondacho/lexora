import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, forkJoin, map, of } from 'rxjs';
import { IncomeReport, ReportsService, SettingsService, VatPayment, VatService } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ACCEPT_PDF, openPdf } from '../../shared/download';
import { ChfPipe, formatDate, SwissDatePipe } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { ofMonth } from '../expenses/expenses.model';
import { blobProblemMessage, problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import {
  afcText,
  booksChange,
  currentSemester,
  deadline,
  meansOf,
  monthlyIncome,
  overlapping,
  Period,
  periodLabel,
  periodStatus,
  samePeriod,
  semester,
  wholeYear,
} from './vat.model';

interface Figures {
  report: IncomeReport;
  inputTax: number | null;
}

/** "Décompte TVA": VAT owed on the money received at the net tax rate, period by period, and the VAT paid. */
@Component({
  selector: 'mbj-vat-page',
  imports: [FormsModule, RouterLink, ChfPipe, SwissDatePipe],
  templateUrl: './vat-page.html',
  styleUrl: './vat-page.css',
})
export class VatPage {
  private readonly reports = inject(ReportsService);
  private readonly vat = inject(VatService);
  private readonly settings = inject(SettingsService);
  private readonly auth = inject(AuthService);

  protected readonly today = todayIso();
  protected readonly can = {
    pay: this.auth.hasGrant('booking:vat_payment'),
    rate: this.auth.hasGrant('booking:parameters_modify'),
  };

  protected readonly period = signal<Period>(currentSemester(this.today));
  protected readonly year = computed(() => Number(this.period().from.slice(0, 4)));
  protected readonly choices = computed(() => {
    const y = this.year();
    return [semester(y, 1), semester(y, 2), wholeYear(y)];
  });
  protected readonly tx = texts;
  protected readonly label = periodLabel;

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly figures = signal<Figures | null>(null);
  protected readonly payments = signal<VatPayment[]>([]);

  protected readonly report = computed(() => this.figures()?.report ?? null);
  protected readonly paid = computed(() => this.payments().find((v) => samePeriod(v, this.period())) ?? null);
  protected readonly overlaps = computed(() => overlapping(this.payments(), this.period()));
  protected readonly status = computed(() => periodStatus(this.period(), this.paid(), this.today));
  protected readonly due = computed(() => deadline(this.period()));
  protected readonly change = computed(() => {
    const r = this.report();
    const p = this.paid();
    return r && p ? booksChange(r, p) : null;
  });
  protected readonly months = computed(() => {
    const r = this.report();
    return r ? monthlyIncome(r, this.period()) : [];
  });
  protected readonly monthlyReserve = computed(() => {
    const list = this.months().filter((m) => m.key <= this.today.slice(0, 7));
    return list.length ? list.reduce((s, m) => s + m.vat, 0) / list.length : 0;
  });

  protected readonly listTab = signal<'invoices' | 'advances'>('invoices');
  protected readonly lines = computed(() => {
    const r = this.report();
    const list = (this.listTab() === 'invoices' ? r?.invoicePayments : r?.advances) ?? [];
    return [...list].sort((a, b) => (b.date ?? '').localeCompare(a.date ?? ''));
  });
  protected readonly means = meansOf;

  // payment form
  protected payDate = this.today;
  protected payAmount: number | null = null;
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly flash = signal<string | null>(null);
  protected readonly confirmDelete = signal(false);

  // rate
  protected readonly editingRate = signal(false);
  protected rateDraft = 0;

  private wantedPayment: number | null = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('paiement')) || null;

  protected customFrom = this.period().from;
  protected customTo = this.period().to;

  constructor() {
    // reload on each period; untracked: load() reads other signals that must not re-run the effect
    effect(() => {
      const p = this.period();
      untracked(() => void this.load(p));
    });
  }

  protected pick(p: Period): void {
    this.flash.set(null);
    this.period.set(p);
  }

  protected moveYear(delta: number): void {
    const p = this.period();
    const y = this.year() + delta;
    this.pick(p.kind === 'S1' ? semester(y, 1) : p.kind === 'S2' ? semester(y, 2) : wholeYear(y));
  }

  protected chooseCustom(): void {
    const p = this.period();
    this.customFrom = p.from;
    this.customTo = p.to;
    this.pick({ kind: 'custom', from: p.from, to: p.to });
  }

  protected applyCustom(): void {
    if (this.customFrom && this.customTo && this.customFrom <= this.customTo) {
      this.pick({ kind: 'custom', from: this.customFrom, to: this.customTo });
    }
  }

  /** Opens the period of a past payment. */
  protected openPayment(v: VatPayment): void {
    const y = Number(v.from.slice(0, 4));
    const known = [semester(y, 1), semester(y, 2), wholeYear(y)].find((p) => p.from === v.from && p.to === v.to);
    this.pick(known ?? { kind: 'custom', from: v.from, to: v.to });
  }

  protected async record(): Promise<void> {
    const p = this.period();
    if (this.busy() || !this.payDate) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.vat.recordVatPayment({
          vatPaymentInput: { from: p.from, to: p.to, paidOn: this.payDate, amount: this.payAmount ?? undefined },
        }),
      );
      await this.load(p);
      this.flash.set(texts().vat.recorded);
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().vat.recordFailed));
    } finally {
      this.busy.set(false);
    }
  }

  protected async removePayment(): Promise<void> {
    const v = this.paid();
    if (!v || this.busy()) {
      return;
    }
    if (!this.confirmDelete()) {
      this.confirmDelete.set(true);
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.vat.deleteVatPayment({ id: v.id }), { defaultValue: null });
      await this.load(this.period());
      this.flash.set(texts().vat.deleted);
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().vat.deleteFailed));
    } finally {
      this.busy.set(false);
      this.confirmDelete.set(false);
    }
  }

  protected editRate(): void {
    this.rateDraft = this.report()?.vatRatePercent ?? 0;
    this.editingRate.set(true);
  }

  protected async saveRate(): Promise<void> {
    if (this.busy() || !(this.rateDraft >= 0 && this.rateDraft <= 100)) {
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.settings.changeBookingSettings({ bookingSettings: { vatRatePercent: this.rateDraft } }));
      this.editingRate.set(false);
      await this.load(this.period());
      this.flash.set(texts().vat.rateChanged(this.rateDraft));
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().vat.rateFailed));
    } finally {
      this.busy.set(false);
    }
  }

  protected async printStatement(): Promise<void> {
    const p = this.period();
    this.error.set(null);
    try {
      const pdf = await firstValueFrom(this.reports.printPeriodStatement({ from: p.from, to: p.to }, 'body', false, ACCEPT_PDF));
      openPdf(pdf, `releves_${p.from}_${p.to}.pdf`);
    } catch (e: unknown) {
      this.error.set(await blobProblemMessage(e, texts().vat.pdfFailed));
    }
  }

  protected async copyFigures(): Promise<void> {
    const r = this.report();
    if (!r) {
      return;
    }
    const text = afcText(this.period(), r);
    try {
      await navigator.clipboard.writeText(text);
      this.flash.set(texts().vat.copied(text.split('\n').slice(1).join(' · ')));
    } catch {
      this.flash.set(text.split('\n').join(' · '));
    }
  }

  protected swiss(iso: string): string {
    return formatDate(iso);
  }

  protected monthOf(iso: string): string {
    return ofMonth(iso.slice(0, 7));
  }

  private async load(p: Period): Promise<void> {
    if (this.state() !== 'ready') {
      this.state.set('loading');
    }
    this.error.set(null);
    this.confirmDelete.set(false);
    try {
      const result = await firstValueFrom(
        forkJoin({
          report: this.reports.getIncomeReport({ from: p.from, to: p.to }),
          inputTax: this.reports.getExpenseReport({ from: p.from, to: p.to }).pipe(
            map((e) => e.taxTotal ?? 0),
            catchError(() => of(null)),
          ),
          payments: this.vat.listVatPayments().pipe(catchError(() => of([] as VatPayment[]))),
        }),
      );
      if (p !== this.period()) {
        return;
      }
      this.figures.set({ report: result.report, inputTax: result.inputTax });
      this.payments.set(result.payments);
      // ?paiement=12 (link from the expenses): open the period of that payment, once
      const wanted = this.wantedPayment;
      this.wantedPayment = null;
      const target = wanted === null ? undefined : result.payments.find((v) => v.id === wanted);
      if (target && !samePeriod(target, p)) {
        this.openPayment(target);
        return;
      }
      this.payAmount = result.report.vatDue ?? null;
      this.payDate = this.today < p.from ? p.from : this.today;
      this.state.set('ready');
    } catch {
      this.state.set('error');
    }
  }
}
