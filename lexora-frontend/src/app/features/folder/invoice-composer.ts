import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs';
import { InvoiceDraft, InvoicesService } from '../../api/generated';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { fromCents, toCents, todayIso, vatCents } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { loadable, Loadable } from '../home/home.model';
import { customerName } from './timeline.model';
import { distribute, toShares } from './shares';

/** "Facturer": bills every open session and deducts every available advance, as the API does. */
@Component({
  selector: 'lx-invoice-composer',
  imports: [FormsModule, ChfPipe, SwissDatePipe],
  template: `
    <section class="card composer" aria-labelledby="invoice-title">
      <div class="composer-head">
        <h2 id="invoice-title">{{ tx().folder.invoice.title }}</h2>
        <button type="button" class="btn btn-small" (click)="cancelled.emit()">{{ tx().common.cancel }}</button>
      </div>
      @switch (draft().state) {
        @case ('loading') {
          <p class="muted">{{ tx().folder.compose.preparing }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().folder.invoice.failedPrepare }}</p>
        }
        @case ('ready') {
          @if (ready(); as d) {
            @if (!(d.honoraries?.length) && !(d.advances?.length)) {
              <p class="muted">{{ tx().folder.invoice.nothing }}</p>
            } @else {
              <table class="lines">
                <tbody>
                  @for (h of d.honoraries; track h.id) {
                    <tr>
                      <td>{{ h.date | swissDate: false }} · {{ h.description }}{{ h.minutes ? ' · ' + h.minutes + ' min' : '' }}</td>
                      <td class="mono">{{ h.amount | chf }}</td>
                    </tr>
                  }
                  <tr class="sum"><td>{{ tx().folder.invoice.honoraries }}</td><td class="mono">{{ d.honorariesSum | chf }}</td></tr>
                  <tr><td>{{ tx().folder.invoice.vat(taxRate()) }}</td><td class="mono">{{ tax() | chf }}</td></tr>
                  @if (advanced() > 0) {
                    <tr><td>{{ tx().folder.invoice.advances(d.advances?.length ?? 0) }}</td><td class="mono">− {{ advanced() | chf }}</td></tr>
                  }
                  <tr class="total"><td>{{ tx().folder.invoice.balance }}</td><td class="mono">{{ net() | chf }}</td></tr>
                </tbody>
              </table>

              <div class="terms">
                <div class="field">
                  <label for="inv-date">{{ tx().folder.compose.date }}</label>
                  <input id="inv-date" type="date" class="input" [(ngModel)]="date" />
                </div>
                <div class="field">
                  <label for="inv-delay">{{ tx().folder.compose.delay }}</label>
                  <input id="inv-delay" type="number" min="0" max="127" class="input" [(ngModel)]="delay" />
                </div>
                <div class="field">
                  <label for="inv-tax">{{ tx().folder.invoice.vatRate }}</label>
                  <input id="inv-tax" type="number" min="0" step="0.1" class="input" [ngModel]="taxRate()" (ngModelChange)="taxRate.set($event ?? 0)" />
                </div>
              </div>

              @if (customers().length > 1 && !hasBilling()) {
                <div class="split">
                  @if (customers().length === 2) {
                    <label for="split">{{ tx().folder.invoice.split }}</label>
                    <input id="split" type="range" min="0" max="100" step="5" [ngModel]="split()" (ngModelChange)="split.set(+$event)" [attr.aria-valuetext]="tx().folder.invoice.splitValue(split(), firstName(0))" />
                    <div class="presets">
                      <button type="button" class="chip" (click)="split.set(50)">50 / 50</button>
                      <button type="button" class="chip" (click)="split.set(100)">{{ tx().folder.invoice.paysAll(firstName(0)) }}</button>
                      <button type="button" class="chip" (click)="split.set(0)">{{ tx().folder.invoice.paysAll(firstName(1)) }}</button>
                    </div>
                  } @else {
                    <p class="muted">{{ tx().folder.invoice.equalShares(customers().length) }}</p>
                  }
                </div>
              }
              <div class="shares">
                @for (s of shares(); track s.customer.id) {
                  <div class="share">
                    <span class="muted">{{ name(s.customer) }}{{ s.customer.billing ? tx().folder.compose.paysAll : '' }}</span>
                    <span class="mono">{{ s.cents / 100 | chf }}</span>
                  </div>
                }
              </div>
              <p class="note muted">{{ tx().folder.invoice.note }}</p>
              @if (error(); as message) {
                <p class="problem" role="alert">{{ message }}</p>
              }
              <button type="button" class="btn btn-primary wide" [disabled]="busy()" (click)="issue()">
                {{ busy() ? tx().folder.invoice.issuing : tx().folder.invoice.issue(net() | chf) }}
              </button>
            }
          }
        }
      }
    </section>
  `,
  styleUrl: './composer.css',
})
export class InvoiceComposer {
  private readonly invoices = inject(InvoicesService);

  readonly folderId = input.required<number>();
  readonly issued = output<number>();
  readonly cancelled = output<void>();

  protected readonly draft = toSignal(
    toObservable(this.folderId).pipe(switchMap((folderId) => loadable(this.invoices.prepareInvoice({ folderId })))),
    { initialValue: { state: 'loading' } as Loadable<InvoiceDraft> },
  );
  protected readonly ready = computed(() => {
    const d = this.draft();
    return d.state === 'ready' ? d.value : null;
  });

  protected readonly tx = texts;
  protected date = todayIso();
  protected delay = 30;
  protected readonly taxRate = signal(8.1);
  /** Percentage paid by the first of two customers. */
  protected readonly split = signal(50);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly customers = computed(() => this.ready()?.customers ?? []);
  protected readonly hasBilling = computed(() => this.customers().some((c) => c.billing));
  protected readonly advanced = computed(() => this.ready()?.advancedSum ?? 0);
  protected readonly tax = computed(() => fromCents(vatCents(toCents(this.ready()?.honorariesSum), this.taxRate() || 0)));
  protected readonly net = computed(() => fromCents(toCents(this.ready()?.honorariesSum) + toCents(this.tax()) - toCents(this.advanced())));
  protected readonly shares = computed(() => {
    const customers = this.customers();
    const weights = customers.length === 2 ? [this.split(), 100 - this.split()] : undefined;
    return distribute(toCents(this.net()), customers, weights);
  });

  constructor() {
    toObservable(this.ready).subscribe((d) => {
      if (d) {
        this.delay = d.defaultDelayDays ?? this.delay;
        this.taxRate.set(d.defaultTaxRatePercent ?? this.taxRate());
      }
    });
  }

  protected name = customerName;

  protected firstName(index: number): string {
    const c = this.customers()[index];
    return c?.firstName || customerName(c);
  }

  protected issue(): void {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.invoices
      .issueInvoice({
        folderId: this.folderId(),
        invoiceTermsInput: {
          date: this.date,
          delayDays: this.delay,
          taxRatePercent: this.taxRate(),
          shares: toShares(this.shares()),
        },
      })
      .subscribe({
        next: (invoice) => this.issued.emit(invoice.id),
        error: (e: unknown) => {
          this.busy.set(false);
          this.error.set(problemMessage(e, texts().folder.invoice.failed));
        },
      });
  }
}
