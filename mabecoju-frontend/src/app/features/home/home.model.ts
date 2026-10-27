import { catchError, map, Observable, of, startWith } from 'rxjs';
import { DebtorsOverview, Month, MonthFigures, OpenDebt, YearOverview } from '../../api/generated';

/** State of a widget's data. */
export type Loadable<T> = { state: 'loading' } | { state: 'ready'; value: T } | { state: 'error' };

export function loadable<T>(source: Observable<T>): Observable<Loadable<T>> {
  return source.pipe(
    map((value): Loadable<T> => ({ state: 'ready', value })),
    catchError(() => of<Loadable<T>>({ state: 'error' })),
    startWith<Loadable<T>>({ state: 'loading' }),
  );
}

/** An overdue invoice (F-…) or advance request (P-…) to recall. */
export interface DebtToRecall {
  kind: 'invoice' | 'request';
  reference: string;
  documentId: number;
  folderId: number;
  folderName: string;
  daysOverdue: number;
  recallCount: number;
  lastRecallDate?: string;
  remaining: number;
  nextRecall: number;
}

export interface Top<T> {
  items: T[];
  /** Number of entries before keeping the top ones. */
  count: number;
  total: number;
}

/** Overdue invoices and requests with something left to pay, the longest overdue first. */
export function debtsToRecall(overview: DebtorsOverview, limit = 5): Top<DebtToRecall> {
  const debts = [
    ...(overview.openInvoices ?? []).map((d) => toRecall(d, 'invoice')),
    ...(overview.openRequests ?? []).map((d) => toRecall(d, 'request')),
  ]
    .filter((d) => d.daysOverdue > 0 && d.remaining > 0)
    .sort((a, b) => b.daysOverdue - a.daysOverdue || b.remaining - a.remaining);
  return top(debts, limit, (d) => d.remaining);
}

function toRecall(debt: OpenDebt, kind: DebtToRecall['kind']): DebtToRecall {
  const recallCount = debt.recallCount ?? 0;
  return {
    kind,
    reference: (kind === 'invoice' ? 'F-' : 'P-') + debt.documentId,
    documentId: debt.documentId ?? 0,
    folderId: debt.folderId ?? 0,
    folderName: debt.folderName ?? '',
    daysOverdue: debt.daysOverdue ?? 0,
    recallCount,
    lastRecallDate: debt.lastRecallDate,
    remaining: debt.remaining ?? 0,
    nextRecall: recallCount + 1,
  };
}

export interface FolderToBill {
  folderId: number;
  folderName: string;
  toBeInvoiced: number;
  available: number;
  uncovered: number;
}

/** Folders with honoraries to invoice, the largest amount not covered by advances first. */
export function foldersToBill(overview: DebtorsOverview, limit = 5): Top<FolderToBill> {
  const folders = (overview.foldersToInvoice ?? [])
    .map((f) => ({
      folderId: f.folderId ?? 0,
      folderName: f.folderName ?? '',
      toBeInvoiced: f.toBeInvoiced ?? 0,
      available: f.available ?? 0,
      uncovered: f.uncovered ?? 0,
    }))
    .filter((f) => f.toBeInvoiced > 0)
    .sort((a, b) => b.uncovered - a.uncovered || b.toBeInvoiced - a.toBeInvoiced);
  return top(folders, limit, (f) => f.uncovered);
}

function top<T>(all: T[], limit: number, amount: (item: T) => number): Top<T> {
  return {
    items: all.slice(0, limit),
    count: all.length,
    total: round(all.reduce((sum, item) => sum + amount(item), 0)),
  };
}

const MONTHS: Month[] = [
  'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
  'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER',
];

export interface MonthSummary {
  year: number;
  /** 0 = January */
  month: number;
  figures: Required<Omit<MonthFigures, 'month'>>;
  previous: Required<Omit<MonthFigures, 'month'>>;
  /** VAT owed on the month's incomes at the net tax rate; null when the rate is unknown. */
  vatDue: number | null;
}

/** Figures of the month of `today` and of the month before, as booked so far. */
export function monthSummary(years: YearOverview[], today: Date, vatRatePercent?: number | null): MonthSummary {
  const year = today.getFullYear();
  const month = today.getMonth();
  const figures = figuresOf(years, year, month);
  const previous = month === 0 ? figuresOf(years, year - 1, 11) : figuresOf(years, year, month - 1);
  const vatDue = vatRatePercent === null || vatRatePercent === undefined ? null : round((figures.incomes * vatRatePercent) / 100);
  return { year, month, figures, previous, vatDue };
}

function figuresOf(years: YearOverview[], year: number, month: number): MonthSummary['figures'] {
  const found = years.find((y) => y.year === year)?.months?.find((m) => m.month === MONTHS[month]);
  return {
    advances: found?.advances ?? 0,
    invoicePayments: found?.invoicePayments ?? 0,
    incomes: found?.incomes ?? 0,
    expenses: found?.expenses ?? 0,
    balance: found?.balance ?? 0,
  };
}

function round(amount: number): number {
  return Math.round(amount * 100) / 100;
}
