import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs';
import { AdvanceRequestDraft, AdvanceRequestsService } from '../../api/generated';
import { ChfPipe } from '../../shared/format';
import { toCents, todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { loadable, Loadable } from '../home/home.model';
import { customerName } from './timeline.model';
import { distribute, toShares } from './shares';

/** "+ Provision": asks the customers for an advance, split equally between them. */
@Component({
  selector: 'mbj-request-composer',
  imports: [FormsModule, ChfPipe],
  template: `
    <section class="card composer" aria-labelledby="request-title">
      <div class="composer-head">
        <h2 id="request-title">Nouvelle demande de provision</h2>
        <button type="button" class="btn btn-small" (click)="cancelled.emit()">Annuler</button>
      </div>
      @switch (draft().state) {
        @case ('loading') {
          <p class="muted">Préparation…</p>
        }
        @case ('error') {
          <p class="problem">La demande n’a pas pu être préparée.</p>
        }
        @case ('ready') {
          @if (ready(); as d) {
            <p class="note muted">
              Honoraires ouverts {{ d.openHonorariesSum | chf }} · provisions disponibles {{ d.availableAdvancesSum | chf }} ·
              non couvert <strong>{{ d.uncoveredHonorariesSum | chf }}</strong>
            </p>
            <div class="field">
              <label for="req-amount">Montant demandé</label>
              <input id="req-amount" type="number" min="0" step="50" class="input" [ngModel]="amount()" (ngModelChange)="amount.set(+$event || 0)" />
            </div>
            <div class="presets">
              @for (p of presets(); track p) {
                <button type="button" class="chip" [class.on]="amount() === p" (click)="amount.set(p)">{{ p | chf }}</button>
              }
            </div>
            <div class="terms">
              <div class="field">
                <label for="req-date">Date</label>
                <input id="req-date" type="date" class="input" [(ngModel)]="date" />
              </div>
              <div class="field">
                <label for="req-delay">Délai (jours)</label>
                <input id="req-delay" type="number" min="0" max="127" class="input" [(ngModel)]="delay" />
              </div>
            </div>
            <div class="shares">
              @for (s of shares(); track s.customer.id) {
                <div class="share">
                  <span class="muted">{{ name(s.customer) }}{{ s.customer.billing ? ' · paie tout' : '' }}</span>
                  <span class="mono">{{ s.cents / 100 | chf }}</span>
                </div>
              }
            </div>
            @if (error(); as message) {
              <p class="problem" role="alert">{{ message }}</p>
            }
            <button type="button" class="btn btn-primary wide" [disabled]="busy() || amount() <= 0" (click)="create()">
              {{ busy() ? 'Enregistrement…' : 'Demander ' + (amount() | chf) }}
            </button>
          }
        }
      }
    </section>
  `,
  styleUrl: './composer.css',
})
export class RequestComposer {
  private readonly requests = inject(AdvanceRequestsService);

  readonly folderId = input.required<number>();
  readonly created = output<number>();
  readonly cancelled = output<void>();

  protected readonly draft = toSignal(
    toObservable(this.folderId).pipe(switchMap((folderId) => loadable(this.requests.prepareAdvanceRequest({ folderId })))),
    { initialValue: { state: 'loading' } as Loadable<AdvanceRequestDraft> },
  );
  protected readonly ready = computed(() => {
    const d = this.draft();
    return d.state === 'ready' ? d.value : null;
  });

  protected date = todayIso();
  protected delay = 10;
  protected readonly amount = signal(0);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  /** What is not covered yet rounded up to 50, then usual amounts. */
  protected readonly presets = computed(() => {
    const uncovered = this.ready()?.uncoveredHonorariesSum ?? 0;
    const list = uncovered > 0 ? [Math.ceil(uncovered / 50) * 50] : [];
    return [...new Set([...list, 400, 800, 1200])];
  });
  protected readonly shares = computed(() => distribute(toCents(this.amount()), this.ready()?.customers ?? []));

  constructor() {
    toObservable(this.ready).subscribe((d) => {
      if (d) {
        this.delay = d.defaultDelayDays ?? this.delay;
        this.amount.set(this.presets()[0]);
      }
    });
  }

  protected name = customerName;

  protected create(): void {
    if (this.busy() || this.amount() <= 0) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.requests
      .createAdvanceRequest({
        folderId: this.folderId(),
        advanceRequestTermsInput: { date: this.date, delayDays: this.delay, amount: this.amount(), shares: toShares(this.shares()) },
      })
      .subscribe({
        next: (request) => this.created.emit(request.id),
        error: (e: unknown) => {
          this.busy.set(false);
          this.error.set(problemMessage(e, 'La demande n’a pas pu être enregistrée.'));
        },
      });
  }
}
