import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { BookingService, OutgoingPayment, OutgoingPaymentInput } from '../../api/generated';
import { ChfPipe, formatChf, formatDate } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { includedVat, kindLabel, VAT_RATES } from './expenses.model';

interface Draft {
  date: string;
  kind: 'BANK' | 'EXPENSE';
  number: string;
  creditor: string;
  title: string;
  amount: number | null;
  rate: number;
  ticket: string;
  paidOn: string;
}

const EMPTY: Draft = { date: todayIso(), kind: 'BANK', number: '', creditor: '', title: '', amount: null, rate: 0, ticket: '', paidOn: '' };

/** New or existing one-off payment; a payment booked by a standing order is shown read-only. */
@Component({
  selector: 'mbj-payment-editor',
  imports: [FormsModule, RouterLink, ChfPipe],
  template: `
    <section class="card editor" aria-labelledby="pay-edit-title">
      @if (vatExpense(); as p) {
        <div class="head">
          <span class="muted small">{{ tx().expenses.editor.vatPayment(swiss(p.date)) }}</span>
          <button type="button" class="btn btn-small" (click)="closed.emit()">{{ tx().common.close }}</button>
        </div>
        <h2 id="pay-edit-title">{{ p.title }}</h2>
        <dl class="facts">
          <dt>Créancier</dt><dd>{{ p.creditor }}</dd>
          <dt>Montant</dt><dd class="mono">{{ p.amount | chf }}</dd>
          <dt>{{ tx().expenses.editor.paidOn }}</dt><dd>{{ swiss(p.paidOn) }}</dd>
        </dl>
        <p class="note muted">{{ tx().expenses.editor.vatPaymentNote }}</p>
        <div class="actions">
          <a class="btn" routerLink="/tva" [queryParams]="{ paiement: p.vatPaymentId }">{{ tx().expenses.editor.seeStatement }}</a>
        </div>
      } @else if (readOnly(); as p) {
        <div class="head">
          <span class="muted small">{{ tx().expenses.editor.orderPayment(swiss(p.date)) }}</span>
          <button type="button" class="btn btn-small" (click)="closed.emit()">{{ tx().common.close }}</button>
        </div>
        <h2 id="pay-edit-title">{{ p.title || p.creditor }}</h2>
        <dl class="facts">
          <dt>Créancier</dt><dd>{{ p.creditor }}</dd>
          <dt>Montant</dt><dd class="mono">{{ p.amount | chf }}</dd>
          <dt>{{ tx().expenses.editor.ofWhichVat(p.taxRatePercent ?? 0) }}</dt><dd class="mono">{{ p.tax | chf }}</dd>
          <dt>{{ tx().expenses.editor.paidOn }}</dt><dd>{{ p.paidOn ? swiss(p.paidOn) : tx().expenses.editor.notYet }}</dd>
        </dl>
        <p class="note muted">{{ tx().expenses.editor.orderPaymentNote }}</p>
        <div class="actions">
          <button type="button" class="btn" (click)="showOrder.emit(p)">{{ tx().expenses.editor.seeOrder }}</button>
          @if (canModify()) {
            <button type="button" class="btn btn-warn" [disabled]="busy()" (click)="remove()">{{ confirming() ? tx().expenses.editor.confirmDelete : tx().expenses.editor.deletePayment }}</button>
          }
        </div>
      } @else {
        <div class="head">
          <h2 id="pay-edit-title">{{ payment() ? tx().expenses.editor.edit : tx().expenses.editor.new }}</h2>
          <button type="button" class="btn btn-small" (click)="closed.emit()">{{ tx().common.close }}</button>
        </div>
        <form class="form" (ngSubmit)="save()">
          <div class="row3">
            <div class="field"><label for="d-date">{{ tx().expenses.date }}</label><input id="d-date" name="date" type="date" class="input" [(ngModel)]="draft.date" /></div>
            <div class="field">
              <label for="d-kind">{{ tx().expenses.type }}</label>
              <select id="d-kind" name="kind" class="input" [(ngModel)]="draft.kind">
                <option value="BANK">{{ tx().expenses.kinds.BANK }}</option>
                <option value="EXPENSE">{{ tx().expenses.kinds.EXPENSE }}</option>
              </select>
            </div>
            <div class="field"><label for="d-no">{{ tx().expenses.editor.voucher }}</label><input id="d-no" name="number" class="input" maxlength="20" [(ngModel)]="draft.number" /></div>
          </div>
          <div class="field"><label for="d-cred">{{ tx().expenses.creditor }}</label><input id="d-cred" name="creditor" class="input" maxlength="80" required [(ngModel)]="draft.creditor" (ngModelChange)="touch()" /></div>
          <div class="field"><label for="d-title">{{ tx().expenses.editor.title }}</label><input id="d-title" name="title" class="input" maxlength="80" [(ngModel)]="draft.title" /></div>
          <div class="row3">
            <div class="field"><label for="d-amount">{{ tx().expenses.editor.amountGross }}</label><input id="d-amount" name="amount" type="number" min="0" step="0.05" class="input" [(ngModel)]="draft.amount" (ngModelChange)="touch()" /></div>
            <div class="field">
              <label for="d-rate">{{ tx().expenses.editor.vatRate }}</label>
              <select id="d-rate" name="rate" class="input" [(ngModel)]="draft.rate" (ngModelChange)="touch()">
                @for (r of rates; track r) {
                  <option [ngValue]="r">{{ r }} %</option>
                }
              </select>
            </div>
            <div class="field"><span class="label-like">{{ tx().expenses.editor.ofVat }}</span><span class="mono vat">{{ vat() | chf }}</span></div>
          </div>
          <div class="field"><label for="d-ref">{{ tx().expenses.editor.ticket }}</label><input id="d-ref" name="ticket" class="input mono" maxlength="80" [(ngModel)]="draft.ticket" /></div>
          <div class="paid">
            <div class="field"><label for="d-paid">{{ tx().expenses.editor.paidOn }}</label><input id="d-paid" name="paidOn" type="date" class="input" [(ngModel)]="draft.paidOn" /></div>
            <button type="button" class="btn btn-small" (click)="draft.paidOn = today">{{ tx().expenses.editor.paidToday }}</button>
            <button type="button" class="btn btn-small" (click)="draft.paidOn = ''">{{ tx().expenses.editor.notPaid }}</button>
          </div>
          @if (invalid() && tried()) {
            <p class="problem">{{ tx().expenses.editor.invalid }}</p>
          }
          @if (error(); as message) {
            <p class="problem" role="alert">{{ message }}</p>
          }
          <div class="actions">
            <button type="submit" class="btn btn-primary" [disabled]="busy() || !canModify()">
              {{ busy() ? tx().common.saving : payment() ? tx().expenses.editor.saveChanges : tx().expenses.editor.add }}
            </button>
            @if (payment() && canModify()) {
              <button type="button" class="btn btn-warn" [disabled]="busy()" (click)="remove()">{{ confirming() ? tx().expenses.editor.confirmDelete : tx().common.delete }}</button>
            }
          </div>
        </form>
      }
    </section>
  `,
  styleUrl: './editor.css',
})
export class PaymentEditor {
  private readonly booking = inject(BookingService);

  /** null: a new payment. */
  protected readonly tx = texts;
  readonly payment = input<OutgoingPayment | null>(null);
  readonly canModify = input(false);
  readonly saved = output<number>();
  readonly deleted = output<void>();
  readonly closed = output<void>();
  readonly showOrder = output<OutgoingPayment>();

  protected readonly rates = VAT_RATES;
  protected readonly today = todayIso();
  protected draft: Draft = { ...EMPTY };
  /** Bumped on each input, to recompute the VAT and the validity of the plain draft object. */
  private readonly version = signal(0);
  protected readonly vat = computed(() => (this.version(), includedVat(this.draft.amount ?? 0, this.draft.rate)));
  protected readonly invalid = computed(() => (this.version(), !this.draft.creditor.trim() || !(this.draft.amount && this.draft.amount > 0)));
  /** A payment booked by a standing order. */
  protected readonly readOnly = computed(() => {
    const p = this.payment();
    return p && p.editable === false ? p : null;
  });
  /** The VAT paid to the AFC, booked by the VAT screen. */
  protected readonly vatExpense = computed(() => {
    const p = this.payment();
    return p && p.vatPaymentId != null ? p : null;
  });
  protected readonly busy = signal(false);
  protected readonly tried = signal(false);
  protected readonly confirming = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly swiss = (iso?: string) => formatDate(iso);

  constructor() {
    effect(() => {
      const p = this.payment();
      this.draft = p
        ? {
            date: p.date ?? todayIso(),
            kind: p.kind === 'EXPENSE' ? 'EXPENSE' : 'BANK',
            number: p.number ?? '',
            creditor: p.creditor ?? '',
            title: p.title ?? '',
            amount: p.amount ?? null,
            rate: p.taxRatePercent ?? 0,
            ticket: p.ticketNumber ?? '',
            paidOn: p.paidOn ?? '',
          }
        : { ...EMPTY, date: todayIso() };
      this.tried.set(false);
      this.confirming.set(false);
      this.error.set(null);
      this.version.update((v) => v + 1);
    });
  }

  protected touch(): void {
    this.version.update((v) => v + 1);
  }

  protected async save(): Promise<void> {
    this.tried.set(true);
    this.touch();
    if (this.invalid() || this.busy()) {
      return;
    }
    const d = this.draft;
    const body: OutgoingPaymentInput = {
      kind: d.kind,
      date: d.date || undefined,
      number: d.number.trim() || undefined,
      creditor: d.creditor.trim(),
      title: d.title.trim() || undefined,
      ticketNumber: d.ticket.replace(/\s+/g, '') || undefined,
      amount: d.amount ?? 0,
      taxRatePercent: d.rate,
      paidOn: d.paidOn || undefined,
    };
    this.busy.set(true);
    this.error.set(null);
    try {
      const existing = this.payment();
      const result = await firstValueFrom(
        existing
          ? this.booking.updateOutgoingPayment({ id: existing.id, outgoingPaymentInput: body })
          : this.booking.recordOfficePayment({ outgoingPaymentInput: body }),
      );
      this.saved.emit(result.id);
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().expenses.editor.saveFailed));
    } finally {
      this.busy.set(false);
    }
  }

  protected async remove(): Promise<void> {
    const p = this.payment();
    if (!p) {
      return;
    }
    if (!this.confirming()) {
      this.confirming.set(true);
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.booking.deleteOutgoingPayment({ id: p.id }), { defaultValue: null });
      this.deleted.emit();
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().expenses.editor.deleteFailed));
    } finally {
      this.busy.set(false);
      this.confirming.set(false);
    }
  }

  protected label = kindLabel;
  protected chf = formatChf;
}
