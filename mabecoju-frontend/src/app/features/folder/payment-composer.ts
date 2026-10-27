import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, of, switchMap } from 'rxjs';
import { AdvancesService, CustomerStatement, DebtorsService, ReceiptLine } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ChfPipe } from '../../shared/format';
import { fromCents, toCents, todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { loadable, Loadable } from '../home/home.model';
import { allocate } from './payment.model';
import { CustomerDue, customerName } from './timeline.model';

type Means = 'cash' | 'transfer';

/**
 * "+ Paiement": money handed over or received outside the bank file, with no payment slip.
 * It settles the customer's oldest documents first; the surplus stays as a spontaneous advance.
 */
@Component({
  selector: 'mbj-payment-composer',
  imports: [FormsModule, ChfPipe],
  template: `
    <section class="card composer" aria-labelledby="payment-title">
      <div class="composer-head">
        <h2 id="payment-title">{{ tx().folder.payment.title }}</h2>
        <button type="button" class="btn btn-small" (click)="cancelled.emit()">{{ tx().common.cancel }}</button>
      </div>
      <p class="note muted">{{ tx().folder.payment.intro }}</p>

      <span class="label">{{ tx().folder.payment.paidBy }}</span>
      <div class="choices" role="group" [attr.aria-label]="tx().folder.payment.client">
        @for (d of dues(); track d.customer.id) {
          <button type="button" class="chip" [class.on]="customerId() === d.customer.id" [attr.aria-pressed]="customerId() === d.customer.id" (click)="pickCustomer(d.customer.id)">
            {{ tx().folder.payment.owes(name(d.customer), d.due | chf) }}
          </button>
        }
      </div>

      <span class="label">{{ tx().folder.payment.means }}</span>
      <div class="choices" role="group" [attr.aria-label]="tx().folder.payment.meansLabel">
        <button type="button" class="chip" [class.on]="means() === 'cash'" [attr.aria-pressed]="means() === 'cash'" (click)="means.set('cash')">{{ tx().folder.timeline.means.cash }}</button>
        <button type="button" class="chip" [class.on]="means() === 'transfer'" [attr.aria-pressed]="means() === 'transfer'" (click)="means.set('transfer')">{{ tx().folder.timeline.means.transfer }}</button>
      </div>
      @if (means() === 'transfer') {
        <div class="field">
          <label for="pay-iban">{{ tx().folder.payment.iban }}</label>
          <input id="pay-iban" class="input mono" maxlength="34" placeholder="CH93 0076 2011 6238 5295 7" [(ngModel)]="iban" />
        </div>
      }

      <div class="terms">
        <div class="field">
          <label for="pay-amount">{{ tx().folder.payment.amount }}</label>
          <input id="pay-amount" type="number" min="0" step="0.05" class="input" [ngModel]="amount()" (ngModelChange)="typeAmount($event)" />
        </div>
        <div class="field">
          <label for="pay-date">{{ tx().folder.compose.date }}</label>
          <input id="pay-date" type="date" class="input" [(ngModel)]="date" />
        </div>
      </div>

      <span class="label">{{ tx().folder.payment.allocation }}</span>
      @switch (statement().state) {
        @case ('loading') {
          <p class="note muted">{{ tx().folder.payment.reading }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().folder.payment.readFailed }}</p>
        }
        @case ('ready') {
          <table class="lines">
            <tbody>
              @for (a of plan().allocations; track a.reference) {
                <tr>
                  <td>{{ a.reference }} · {{ a.kind === 'invoice' ? tx().folder.payment.invoice : tx().folder.payment.request }} · {{ a.settles ? tx().folder.payment.settled : tx().folder.payment.partial }}</td>
                  <td class="mono">{{ a.cents / 100 | chf }}</td>
                </tr>
              }
              @if (plan().surplusCents > 0) {
                <tr><td>{{ tx().folder.payment.surplus }}</td><td class="mono">{{ plan().surplusCents / 100 | chf }}</td></tr>
              }
            </tbody>
          </table>
          @if (!plan().allocations.length && amount() > 0) {
            <p class="note muted">{{ tx().folder.payment.nothingOpen }}</p>
          }
          @if (plan().surplusCents > 0 && !canAdvance) {
            <p class="problem">{{ tx().folder.payment.noAdvanceRight }}</p>
          }
          <p class="note muted">{{ tx().folder.payment.rule }}</p>
        }
      }
      @if (error(); as message) {
        <p class="problem" role="alert">{{ message }}</p>
      }
      <button type="button" class="btn btn-primary wide" [disabled]="!canSave()" (click)="save()">
        {{ busy() ? tx().common.saving : tx().folder.payment.record(amount() | chf, means() === 'cash') }}
      </button>
    </section>
  `,
  styleUrl: './composer.css',
})
export class PaymentComposer {
  private readonly debtors = inject(DebtorsService);
  private readonly advances = inject(AdvancesService);

  readonly folderId = input.required<number>();
  readonly dues = input.required<CustomerDue[]>();
  readonly recorded = output<void>();
  /** Part of the payment was saved: the folder must be reloaded although the form stays open. */
  readonly changed = output<void>();
  readonly cancelled = output<void>();

  protected readonly canAdvance = inject(AuthService).hasGrant('invoicing:advance_receipt');
  private readonly chosen = signal<number | null>(null);
  /** The customer chosen, or the first one who owes something. */
  protected readonly customerId = computed(
    () => this.chosen() ?? (this.dues().find((d) => d.due > 0) ?? this.dues()[0])?.customer.id ?? null,
  );
  protected readonly means = signal<Means>('cash');
  protected readonly tx = texts;
  protected iban = '';
  protected date = todayIso();
  private readonly typed = signal<number | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Bumped to read the statement again after part of a payment was saved. */
  private readonly refresh = signal(0);
  private readonly statementKey = computed(() => ({ customerId: this.customerId(), refresh: this.refresh() }));

  protected readonly statement = toSignal(
    toObservable(this.statementKey).pipe(
      switchMap(({ customerId }) =>
        customerId === null
          ? of<Loadable<CustomerStatement>>({ state: 'ready', value: {} })
          : loadable(this.debtors.getCustomerStatement({ folderId: this.folderId(), customerId })),
      ),
    ),
    { initialValue: { state: 'loading' } as Loadable<CustomerStatement> },
  );
  private readonly owed = computed(() => {
    const s = this.statement();
    return s.state === 'ready' ? s.value : null;
  });
  /** What the customer typed, or by default all he owes. */
  protected readonly amount = computed(() => this.typed() ?? this.owed()?.totalRemaining ?? 0);
  protected readonly plan = computed(() => allocate(toCents(this.amount()), this.owed()));
  protected readonly canSave = computed(
    () =>
      !this.busy() &&
      this.customerId() !== null &&
      this.statement().state === 'ready' &&
      toCents(this.amount()) > 0 &&
      (this.plan().surplusCents === 0 || this.canAdvance),
  );

  protected name = customerName;

  protected pickCustomer(id: number): void {
    this.chosen.set(id);
    this.typed.set(null);
  }

  protected typeAmount(value: number | null): void {
    this.typed.set(value === null ? 0 : Math.max(0, value));
  }

  protected async save(): Promise<void> {
    const customerId = this.customerId();
    if (!this.canSave() || customerId === null) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const cash = this.means() === 'cash';
    const iban = cash ? undefined : this.iban.replace(/\s+/g, '').toUpperCase() || undefined;
    const plan = this.plan();
    const line = (documentId: number, cents: number): ReceiptLine => ({ documentId, date: this.date, amount: fromCents(cents), cash, iban });
    let receiptsSaved = false;
    try {
      if (plan.allocations.length) {
        await firstValueFrom(
          this.debtors.receivePayments({
            folderId: this.folderId(),
            customerId,
            receiptInput: {
              invoices: plan.allocations.filter((a) => a.kind === 'invoice').map((a) => line(a.documentId, a.cents)),
              requests: plan.allocations.filter((a) => a.kind === 'request').map((a) => line(a.documentId, a.cents)),
            },
          }),
        );
        receiptsSaved = true;
      }
      if (plan.surplusCents > 0) {
        await firstValueFrom(
          this.advances.receiveAdvance({
            folderId: this.folderId(),
            advanceInput: { customerId, date: this.date, amount: fromCents(plan.surplusCents), cash, iban },
          }),
        );
      }
      this.recorded.emit();
    } catch (e: unknown) {
      this.busy.set(false);
      if (receiptsSaved) {
        // the documents are paid; only the surplus is missing: show it and let the user record it again
        this.changed.emit();
        this.refresh.update((n) => n + 1);
        this.typed.set(fromCents(plan.surplusCents));
        this.error.set(texts().folder.payment.surplusFailed(problemMessage(e)));
      } else {
        this.error.set(problemMessage(e, texts().folder.payment.failed));
      }
    }
  }
}
