import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, of, switchMap } from 'rxjs';
import { AdvancesService, CustomerStatement, DebtorsService, ReceiptLine } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ChfPipe } from '../../shared/format';
import { fromCents, toCents, todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
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
        <h2 id="payment-title">Recevoir un paiement</h2>
        <button type="button" class="btn btn-small" (click)="cancelled.emit()">Annuler</button>
      </div>
      <p class="note muted">Paiement remis en main propre ou reçu hors fichier bancaire.</p>

      <span class="label">Payé par</span>
      <div class="choices" role="group" aria-label="Client">
        @for (d of dues(); track d.customer.id) {
          <button type="button" class="chip" [class.on]="customerId() === d.customer.id" [attr.aria-pressed]="customerId() === d.customer.id" (click)="pickCustomer(d.customer.id)">
            {{ name(d.customer) }} · doit {{ d.due | chf }}
          </button>
        }
      </div>

      <span class="label">Moyen</span>
      <div class="choices" role="group" aria-label="Moyen de paiement">
        <button type="button" class="chip" [class.on]="means() === 'cash'" [attr.aria-pressed]="means() === 'cash'" (click)="means.set('cash')">Comptant</button>
        <button type="button" class="chip" [class.on]="means() === 'transfer'" [attr.aria-pressed]="means() === 'transfer'" (click)="means.set('transfer')">Virement</button>
      </div>
      @if (means() === 'transfer') {
        <div class="field">
          <label for="pay-iban">IBAN du payeur (facultatif)</label>
          <input id="pay-iban" class="input mono" maxlength="34" placeholder="CH93 0076 2011 6238 5295 7" [(ngModel)]="iban" />
        </div>
      }

      <div class="terms">
        <div class="field">
          <label for="pay-amount">Montant reçu</label>
          <input id="pay-amount" type="number" min="0" step="0.05" class="input" [ngModel]="amount()" (ngModelChange)="typeAmount($event)" />
        </div>
        <div class="field">
          <label for="pay-date">Date</label>
          <input id="pay-date" type="date" class="input" [(ngModel)]="date" />
        </div>
      </div>

      <span class="label">Imputation</span>
      @switch (statement().state) {
        @case ('loading') {
          <p class="note muted">Lecture de ce que doit le client…</p>
        }
        @case ('error') {
          <p class="problem">Ce que doit le client n’a pas pu être lu.</p>
        }
        @case ('ready') {
          <table class="lines">
            <tbody>
              @for (a of plan().allocations; track a.reference) {
                <tr>
                  <td>{{ a.reference }} · {{ a.kind === 'invoice' ? 'facture' : 'provision' }} · {{ a.settles ? 'soldée' : 'partiel' }}</td>
                  <td class="mono">{{ a.cents / 100 | chf }}</td>
                </tr>
              }
              @if (plan().surplusCents > 0) {
                <tr><td>Avance spontanée (provision)</td><td class="mono">{{ plan().surplusCents / 100 | chf }}</td></tr>
              }
            </tbody>
          </table>
          @if (!plan().allocations.length && amount() > 0) {
            <p class="note muted">Rien d’ouvert pour ce client : le montant devient une avance spontanée, déduite à la prochaine facture.</p>
          }
          @if (plan().surplusCents > 0 && !canAdvance) {
            <p class="problem">Vos droits ne permettent pas d’enregistrer le surplus comme avance.</p>
          }
          <p class="note muted">Les documents les plus anciens sont soldés en premier ; le surplus reste en provision.</p>
        }
      }
      @if (error(); as message) {
        <p class="problem" role="alert">{{ message }}</p>
      }
      <button type="button" class="btn btn-primary wide" [disabled]="!canSave()" (click)="save()">
        {{ busy() ? 'Enregistrement…' : 'Enregistrer ' + (amount() | chf) + ' · ' + (means() === 'cash' ? 'comptant' : 'virement') }}
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
        this.error.set('Les documents sont payés, mais le surplus n’a pas pu être enregistré comme avance : ' + problemMessage(e));
      } else {
        this.error.set(problemMessage(e, 'Le paiement n’a pas pu être enregistré.'));
      }
    }
  }
}
