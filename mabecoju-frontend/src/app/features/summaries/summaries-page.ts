import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, forkJoin, map, of } from 'rxjs';
import { ReportsService, SettingsService, UserStatistics, UsersService, UserSummary, YearOverview } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ACCEPT_PDF, openPdf } from '../../shared/download';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { fromCents, todayIso, toCents } from '../../shared/money';
import { blobProblemMessage } from '../../shared/problem';
import { shiftMonth } from '../expenses/expenses.model';
import { texts } from '../../i18n/i18n';
import { change, yearSummary } from './summaries.model';

type Tab = 'overview' | 'people' | 'statements';

interface Range {
  key: string;
  label: () => string;
  from: string;
  to: string;
}

interface Person {
  user: UserSummary;
  stats: UserStatistics | null;
}

const lastDayOf = (month: string) => {
  const [y, m] = month.split('-').map(Number);
  return `${month}-${String(new Date(y, m, 0).getDate()).padStart(2, '0')}`;
};

/** "Synthèses": incomes and expenses month by month, activity per person, printable statements of a period. */
@Component({
  selector: 'mbj-summaries-page',
  imports: [FormsModule, RouterLink, ChfPipe, SwissDatePipe],
  templateUrl: './summaries-page.html',
  styleUrl: './summaries-page.css',
})
export class SummariesPage {
  private readonly reports = inject(ReportsService);
  private readonly users = inject(UsersService);
  private readonly settings = inject(SettingsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly today = todayIso();
  private readonly thisYear = Number(this.today.slice(0, 4));
  protected readonly tx = texts;
  protected readonly can = {
    overview: this.auth.hasGrant('booking:overview'),
    people: this.auth.hasGrant('booking:statistics_search'),
    statements: this.auth.hasGrant('booking:tax_summary'),
  };
  protected readonly tabs = (
    [
      { value: 'overview', allowed: this.can.overview },
      { value: 'people', allowed: this.can.people },
      { value: 'statements', allowed: this.can.statements },
    ] as { value: Tab; allowed: boolean }[]
  ).filter((t) => t.allowed);

  private readonly route = inject(ActivatedRoute);
  protected readonly tab = toSignal(
    this.route.queryParamMap.pipe(
      map((q): Tab => {
        const wanted = q.get('onglet') === 'personnes' ? 'people' : q.get('onglet') === 'releves' ? 'statements' : 'overview';
        return this.tabs.some((t) => t.value === wanted) ? wanted : this.tabs[0]?.value ?? 'overview';
      }),
    ),
    { initialValue: 'overview' as Tab },
  );

  // ----- monthly overview -----------------------------------------------------------------------------------
  protected readonly overview = signal<{ state: 'loading' | 'ready' | 'error'; years: YearOverview[]; rate: number | null }>({
    state: 'loading',
    years: [],
    rate: null,
  });
  protected readonly year = signal(this.thisYear);
  protected readonly yearChoices = computed(() => {
    const known = this.overview().years.map((y) => y.year ?? 0);
    return [...new Set([this.thisYear, ...known])].sort((a, b) => b - a).slice(0, 6);
  });
  protected readonly summary = computed(() => yearSummary(this.overview().years, this.year(), this.today));
  protected readonly incomeChange = computed(() => change(this.summary().total.incomes, this.summary().previousIncomes));
  protected readonly vatDue = computed(() => {
    const rate = this.overview().rate;
    return rate === null ? null : fromCents(Math.round((toCents(this.summary().total.incomes) * rate) / 100));
  });

  // ----- per person -----------------------------------------------------------------------------------------
  protected readonly peopleRanges: Range[] = [
    { key: 'year', label: () => texts().summaries.ranges.yearOf(this.thisYear), from: `${this.thisYear}-01-01`, to: `${this.thisYear}-12-31` },
    { key: 'last', label: () => texts().summaries.ranges.yearOf(this.thisYear - 1), from: `${this.thisYear - 1}-01-01`, to: `${this.thisYear - 1}-12-31` },
    {
      key: '12m',
      label: () => texts().summaries.ranges.last12,
      from: shiftMonth(this.today.slice(0, 7), -11) + '-01',
      to: lastDayOf(this.today.slice(0, 7)),
    },
  ];
  protected readonly peopleRange = signal<Range>(this.peopleRanges[0]);
  protected readonly people = signal<{ state: 'idle' | 'loading' | 'ready' | 'error'; list: Person[]; onlyMe: boolean }>({
    state: 'idle',
    list: [],
    onlyMe: false,
  });
  protected readonly incomesOfAll = computed(() => this.people().list.reduce((s, p) => s + toCents(p.stats?.incomesTotal), 0));

  // ----- statements ------------------------------------------------------------------------------------------
  protected readonly presets: Range[] = (() => {
    const month = this.today.slice(0, 7);
    const previous = shiftMonth(month, -1);
    const half = Number(month.slice(5)) <= 6 ? 1 : 2;
    return [
      { key: 'month', label: () => texts().summaries.ranges.month, from: month + '-01', to: lastDayOf(month) },
      { key: 'previous', label: () => texts().summaries.ranges.previous, from: previous + '-01', to: lastDayOf(previous) },
      {
        key: 'half',
        label: () => (half === 1 ? texts().summaries.ranges.half1 : texts().summaries.ranges.half2),
        from: `${this.thisYear}-${half === 1 ? '01' : '07'}-01`,
        to: `${this.thisYear}-${half === 1 ? '06-30' : '12-31'}`,
      },
      { key: 'year', label: () => texts().summaries.ranges.yearOf(this.thisYear), from: `${this.thisYear}-01-01`, to: `${this.thisYear}-12-31` },
    ];
  })();
  protected statementFrom = this.presets[1].from;
  protected statementTo = this.presets[1].to;
  protected readonly statementRange = signal({ from: this.presets[1].from, to: this.presets[1].to });
  protected readonly statement = signal<{ state: 'idle' | 'loading' | 'ready' | 'error'; incomes: number; vat: number; rate: number; expenses: number; incomeCount: number; expenseCount: number }>({
    state: 'idle', incomes: 0, vat: 0, rate: 0, expenses: 0, incomeCount: 0, expenseCount: 0,
  });
  protected readonly printing = signal(false);
  protected readonly printError = signal<string | null>(null);
  protected readonly printed = signal<string | null>(null);

  constructor() {
    if (this.can.overview) {
      void this.loadOverview();
    }
    effect(() => {
      const tab = this.tab();
      const range = this.peopleRange();
      untracked(() => {
        if (tab === 'people') {
          void this.loadPeople(range);
        }
      });
    });
    effect(() => {
      const tab = this.tab();
      const range = this.statementRange();
      untracked(() => {
        if (tab === 'statements') {
          void this.loadStatement(range.from, range.to);
        }
      });
    });
  }

  protected setTab(tab: Tab): void {
    const onglet = tab === 'people' ? 'personnes' : tab === 'statements' ? 'releves' : null;
    void this.router.navigate([], { queryParams: { onglet }, replaceUrl: true });
  }

  protected pickPreset(r: Range): void {
    this.statementFrom = r.from;
    this.statementTo = r.to;
    this.applyRange();
  }

  protected applyRange(): void {
    if (this.statementFrom && this.statementTo && this.statementFrom <= this.statementTo) {
      this.printed.set(null);
      this.statementRange.set({ from: this.statementFrom, to: this.statementTo });
    }
  }

  protected isPreset(r: Range): boolean {
    const s = this.statementRange();
    return s.from === r.from && s.to === r.to;
  }

  protected share(p: Person): number {
    const all = this.incomesOfAll();
    return all ? Math.round((toCents(p.stats?.incomesTotal) * 1000) / all) / 10 : 0;
  }

  protected initials(u: UserSummary): string {
    return u.shortcut || ((u.firstName?.[0] ?? '') + (u.lastName?.[0] ?? '')).toLowerCase() || u.username || '?';
  }

  protected fullName(u: UserSummary): string {
    return [u.firstName, u.lastName].filter(Boolean).join(' ') || u.username || '';
  }

  protected async download(): Promise<void> {
    const { from, to } = this.statementRange();
    this.printing.set(true);
    this.printError.set(null);
    try {
      const pdf = await firstValueFrom(this.reports.printPeriodStatement({ from, to }, 'body', false, ACCEPT_PDF));
      const name = `releves_${from}_${to}.pdf`;
      openPdf(pdf, name);
      this.printed.set(name);
    } catch (e: unknown) {
      this.printError.set(await blobProblemMessage(e, texts().summaries.pdfFailed));
    } finally {
      this.printing.set(false);
    }
  }

  private async loadOverview(): Promise<void> {
    try {
      const result = await firstValueFrom(
        forkJoin({
          years: this.reports.getMonthlyOverview(),
          rate: this.settings.getBookingSettings().pipe(
            map((s) => s.vatRatePercent ?? null),
            catchError(() => of(null)),
          ),
        }),
      );
      this.overview.set({ state: 'ready', years: result.years, rate: result.rate });
    } catch {
      this.overview.set({ state: 'error', years: [], rate: null });
    }
  }

  /** Everyone when the user may list the users, otherwise his own figures. */
  private async loadPeople(range: Range): Promise<void> {
    this.people.update((p) => ({ ...p, state: 'loading' }));
    const me = this.auth.user();
    const self: UserSummary = { username: me?.username, firstName: me?.firstName, lastName: me?.lastName, shortcut: me?.shortcut };
    let users: UserSummary[];
    let onlyMe = false;
    try {
      users = await firstValueFrom(this.users.listUsers());
    } catch {
      users = [self];
      onlyMe = true;
    }
    try {
      const list = await Promise.all(
        users.map(async (user) => ({
          user,
          stats: await firstValueFrom(
            this.reports.getUserStatistics({ username: user.username ?? '', from: range.from, to: range.to }).pipe(catchError(() => of(null))),
          ),
        })),
      );
      if (range !== this.peopleRange()) {
        return;
      }
      // people without any folder in charge are left out
      const active = list.filter((p) => (p.stats?.folders?.length ?? 0) > 0 || toCents(p.stats?.incomesTotal) > 0);
      active.sort((a, b) => toCents(b.stats?.incomesTotal) - toCents(a.stats?.incomesTotal));
      this.people.set({ state: 'ready', list: active.length ? active : list.slice(0, 1), onlyMe });
    } catch {
      this.people.set({ state: 'error', list: [], onlyMe });
    }
  }

  private async loadStatement(from: string, to: string): Promise<void> {
    this.statement.update((s) => ({ ...s, state: 'loading' }));
    try {
      const r = await firstValueFrom(
        forkJoin({ incomes: this.reports.getIncomeReport({ from, to }), expenses: this.reports.getExpenseReport({ from, to }) }),
      );
      const current = this.statementRange();
      if (current.from !== from || current.to !== to) {
        return;
      }
      this.statement.set({
        state: 'ready',
        incomes: r.incomes.total ?? 0,
        vat: r.incomes.vatDue ?? 0,
        rate: r.incomes.vatRatePercent ?? 0,
        expenses: r.expenses.total ?? 0,
        incomeCount: (r.incomes.invoicePayments?.length ?? 0) + (r.incomes.advances?.length ?? 0),
        expenseCount: r.expenses.payments?.length ?? 0,
      });
    } catch {
      this.statement.update((s) => ({ ...s, state: 'error' }));
    }
  }
}
