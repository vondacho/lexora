import { Month, YearOverview } from '../../api/generated';
import { fromCents, toCents } from '../../shared/money';
import { monthNames } from '../../i18n/i18n';

const MONTHS: Month[] = [
  'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
  'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER',
];

export interface MonthRow {
  label: string;
  advances: number;
  invoices: number;
  incomes: number;
  expenses: number;
  balance: number;
  /** Bars, as a share of the largest income or expense of the year. */
  incomeRatio: number;
  expenseRatio: number;
  /** Month not reached yet in the current year. */
  future: boolean;
}

export interface Figures {
  advances: number;
  invoices: number;
  incomes: number;
  expenses: number;
  balance: number;
}

export interface YearSummary {
  year: number;
  rows: MonthRow[];
  total: Figures;
  /** Per month, over the months elapsed (all twelve for a past year). */
  average: Figures;
  monthsCounted: number;
  /** Incomes of the same months the year before; null when that year has no data. */
  previousIncomes: number | null;
}

/** A year of the monthly overview, compared with the same months of the year before. */
export function yearSummary(years: YearOverview[], year: number, today: string): YearSummary {
  const [thisYear, thisMonth] = today.split('-').map(Number);
  const counted = year < thisYear ? 12 : year === thisYear ? thisMonth : 0;
  const find = (y: number, i: number) => years.find((x) => x.year === y)?.months?.find((m) => m.month === MONTHS[i]);
  const raw = MONTHS.map((_, i) => {
    const m = find(year, i);
    return {
      advances: toCents(m?.advances),
      invoices: toCents(m?.invoicePayments),
      incomes: toCents(m?.incomes),
      expenses: toCents(m?.expenses),
      balance: toCents(m?.balance),
    };
  });
  const max = Math.max(1, ...raw.map((r) => Math.max(r.incomes, r.expenses)));
  const rows = raw.map((r, i) => ({
    label: monthNames()[i],
    advances: fromCents(r.advances),
    invoices: fromCents(r.invoices),
    incomes: fromCents(r.incomes),
    expenses: fromCents(r.expenses),
    balance: fromCents(r.balance),
    incomeRatio: r.incomes / max,
    expenseRatio: r.expenses / max,
    future: i >= counted,
  }));
  const sum = (key: keyof Figures) => raw.reduce((s, r) => s + r[key], 0);
  const keys: (keyof Figures)[] = ['advances', 'invoices', 'incomes', 'expenses', 'balance'];
  const total = Object.fromEntries(keys.map((k) => [k, fromCents(sum(k))])) as unknown as Figures;
  const average = Object.fromEntries(keys.map((k) => [k, counted ? fromCents(Math.round(sum(k) / counted)) : 0])) as unknown as Figures;
  const previous = years.find((x) => x.year === year - 1);
  const previousIncomes = previous
    ? fromCents(MONTHS.slice(0, counted || 12).reduce((s, _, i) => s + toCents(find(year - 1, i)?.incomes), 0))
    : null;
  return { year, rows, total, average, monthsCounted: counted, previousIncomes };
}

/** "+14.5 %" compared with the year before; null without a base. */
export function change(current: number, previous: number | null): number | null {
  if (previous === null || previous === 0) {
    return null;
  }
  return Math.round(((current - previous) / previous) * 1000) / 10;
}
