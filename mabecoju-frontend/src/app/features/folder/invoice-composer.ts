import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs';
import { InvoiceDraft, InvoicesService } from '../../api/generated';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { fromCents, toCents, todayIso, vatCents } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { loadable, Loadable } from '../home/home.model';
import { customerName } from './timeline.model';
import { distribute, toShares } from './shares';

/** "Facturer": bills every open session and deducts every available advance, as the API does. */
@Component({
  selector: 'mbj-invoice-composer',
  imports: [FormsModule, ChfPipe, SwissDatePipe],
  template: `
    <section class="card composer" aria-labelledby="invoice-title">
      <div class="composer-head">
        <h2 id="invoice-title">Nouvelle facture</h2>
        <button type="button" class="btn btn-small" (click)="cancelled.emit()">Annuler</button>
      </div>
      @switch (draft().state) {
        @case ('loading') {
          <p class="muted">Préparation…</p>
        }
        @case ('error') {
          <p class="problem">La facture n’a pas pu être préparée.</p>
        }
        @case ('ready') {
          @if (ready(); as d) {
            @if (!(d.honoraries?.length) && !(d.advances?.length)) {
              <p class="muted">Rien à facturer : aucune séance ouverte ni provision disponible.</p>
            } @else {
              <table class="lines">
                <tbody>
                  @for (h of d.honoraries; track h.id) {
                    <tr>
                      <td>{{ h.date | swissDate: false }} · {{ h.description }}{{ h.minutes ? ' · ' + h.minutes + ' min' : '' }}</td>
                      <td class="mono">{{ h.amount | chf }}</td>
                    </tr>
                  }
                  <tr class="sum"><td>Honoraires</td><td class="mono">{{ d.honorariesSum | chf }}</td></tr>
                  <tr><td>TVA {{ taxRate() }} %</td><td class="mono">{{ tax() | chf }}</td></tr>
                  @if (advanced() > 0) {
                    <tr><td>Provisions déduites ({{ d.advances?.length }})</td><td class="mono">− {{ advanced() | chf }}</td></tr>
                  }
                  <tr class="total"><td>Solde à payer</td><td class="mono">{{ net() | chf }}</td></tr>
                </tbody>
              </table>

              <div class="terms">
                <div class="field">
                  <label for="inv-date">Date</label>
                  <input id="inv-date" type="date" class="input" [(ngModel)]="date" />
                </div>
                <div class="field">
                  <label for="inv-delay">Délai (jours)</label>
                  <input id="inv-delay" type="number" min="0" max="127" class="input" [(ngModel)]="delay" />
                </div>
                <div class="field">
                  <label for="inv-tax">TVA (%)</label>
                  <input id="inv-tax" type="number" min="0" step="0.1" class="input" [ngModel]="taxRate()" (ngModelChange)="taxRate.set($event ?? 0)" />
                </div>
              </div>

              @if (customers().length > 1 && !hasBilling()) {
                <div class="split">
                  @if (customers().length === 2) {
                    <label for="split">Répartition entre les clients</label>
                    <input id="split" type="range" min="0" max="100" step="5" [ngModel]="split()" (ngModelChange)="split.set(+$event)" [attr.aria-valuetext]="split() + ' % pour ' + firstName(0)" />
                    <div class="presets">
                      <button type="button" class="chip" (click)="split.set(50)">50 / 50</button>
                      <button type="button" class="chip" (click)="split.set(100)">{{ firstName(0) }} paie tout</button>
                      <button type="button" class="chip" (click)="split.set(0)">{{ firstName(1) }} paie tout</button>
                    </div>
                  } @else {
                    <p class="muted">Parts égales entre les {{ customers().length }} clients.</p>
                  }
                </div>
              }
              <div class="shares">
                @for (s of shares(); track s.customer.id) {
                  <div class="share">
                    <span class="muted">{{ name(s.customer) }}{{ s.customer.billing ? ' · paie tout' : '' }}</span>
                    <span class="mono">{{ s.cents / 100 | chf }}</span>
                  </div>
                }
              </div>
              <p class="note muted">Parts arrondies à 5 centimes. La facture s’imprime ensuite depuis le fil du dossier.</p>
              @if (error(); as message) {
                <p class="problem" role="alert">{{ message }}</p>
              }
              <button type="button" class="btn btn-primary wide" [disabled]="busy()" (click)="issue()">
                {{ busy() ? 'Émission…' : 'Émettre la facture de ' + (net() | chf) }}
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
          this.error.set(problemMessage(e, 'La facture n’a pas pu être émise.'));
        },
      });
  }
}
