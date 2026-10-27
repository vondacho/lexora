import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, map, of } from 'rxjs';
import { ReportsService, SettingsService } from '../../api/generated';
import { ChfPipe } from '../../shared/format';
import { monthNames, texts } from '../../i18n/i18n';
import { loadable, monthSummary } from './home.model';


@Component({
  selector: 'mbj-month-widget',
  imports: [ChfPipe],
  template: `
    <section class="card widget" aria-labelledby="month-title">
      <div class="widget-head">
        <div>
          <h2 id="month-title">{{ title() }}</h2>
          <p class="muted">{{ tx().home.month.asOf(today.getDate(), monthName(), previousName()) }}</p>
        </div>
      </div>
      @switch (data().state) {
        @case ('loading') {
          <p class="empty">{{ tx().common.loading }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().home.month.failed }}</p>
        }
        @case ('ready') {
          @if (summary(); as s) {
            <div class="tiles">
              <div class="tile in">
                <span class="label">{{ tx().home.month.incomes }}</span>
                <span class="value">{{ s.figures.incomes | chf }}</span>
                <span class="note">{{ tx().home.month.incomesDetail(s.figures.invoicePayments | chf, s.figures.advances | chf) }}</span>
                <span class="note">{{ previousName() }} : {{ s.previous.incomes | chf }}</span>
              </div>
              <div class="tile">
                <span class="label">{{ tx().home.month.expenses }}</span>
                <span class="value">{{ s.figures.expenses | chf }}</span>
                <span class="note">{{ previousName() }} : {{ s.previous.expenses | chf }}</span>
              </div>
              <div class="tile vat">
                <span class="label">{{ tx().home.month.vat }}</span>
                <span class="value">{{ s.vatDue | chf }}</span>
                <span class="note">{{ rate() !== null ? tx().home.month.vatRate(rate()!) : tx().home.month.vatUnknown }}</span>
              </div>
              <div class="tile">
                <span class="label">{{ tx().home.month.balance }}</span>
                <span class="value" [class.negative]="s.figures.balance < 0">{{ s.figures.balance | chf }}</span>
                <span class="note">{{ tx().home.month.balanceNote }}</span>
              </div>
            </div>
          }
        }
      }
    </section>
  `,
  styles: `
    .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
    .tile { display: flex; flex-direction: column; gap: 2px; padding: 12px; border-radius: var(--radius); background: var(--ground); min-width: 0; }
    .tile.in { background: var(--ok-tint); }
    .tile.vat { background: var(--warn-tint); }
    .label { font-size: 12px; color: var(--muted); }
    .value { font-family: var(--mono); font-size: 20px; font-weight: 500; }
    .value.negative { color: var(--warn); }
    .note { font-size: 12px; color: var(--muted); }
  `,
})
export class MonthWidget {
  private readonly reports = inject(ReportsService);
  private readonly settings = inject(SettingsService);

  protected readonly tx = texts;
  protected readonly today = new Date();
  protected readonly monthName = computed(() => monthNames()[this.today.getMonth()]);
  protected readonly previousName = computed(() => monthNames()[(this.today.getMonth() + 11) % 12]);
  protected readonly title = computed(() => capitalize(this.monthName()) + ' ' + this.today.getFullYear());

  protected readonly data = toSignal(
    loadable(
      forkJoin({
        years: this.reports.getMonthlyOverview(),
        // without the rate the month is still shown, only the VAT is unknown
        rate: this.settings.getBookingSettings().pipe(
          map((s) => s.vatRatePercent ?? null),
          catchError(() => of(null)),
        ),
      }),
    ),
    { initialValue: { state: 'loading' } as const },
  );

  protected readonly rate = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? data.value.rate : null;
  });

  protected readonly summary = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? monthSummary(data.value.years, this.today, data.value.rate) : null;
  });
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
