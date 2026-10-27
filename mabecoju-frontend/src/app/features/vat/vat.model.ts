import { IncomeLine, IncomeReport, VatPayment } from '../../api/generated';
import { formatDate } from '../../shared/format';
import { fromCents, toCents } from '../../shared/money';

export type PeriodKind = 'S1' | 'S2' | 'year' | 'custom';

export interface Period {
  kind: PeriodKind;
  from: string;
  to: string;
}

const MONTHS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

const pad = (n: number) => String(n).padStart(2, '0');
const lastDay = (year: number, month: number) => new Date(year, month, 0).getDate(); // month 1-12

export function semester(year: number, half: 1 | 2): Period {
  return half === 1
    ? { kind: 'S1', from: `${year}-01-01`, to: `${year}-06-30` }
    : { kind: 'S2', from: `${year}-07-01`, to: `${year}-12-31` };
}

export function wholeYear(year: number): Period {
  return { kind: 'year', from: `${year}-01-01`, to: `${year}-12-31` };
}

/** The semester of the day: the AFC settles the net tax rate method twice a year. */
export function currentSemester(today: string): Period {
  const [year, month] = today.split('-').map(Number);
  return semester(year, month <= 6 ? 1 : 2);
}

export function periodLabel(p: Period): string {
  const year = p.from.slice(0, 4);
  switch (p.kind) {
    case 'S1':
      return `1er semestre ${year}`;
    case 'S2':
      return `2e semestre ${year}`;
    case 'year':
      return `Année ${year}`;
    default:
      return `${formatDate(p.from)} – ${formatDate(p.to)}`;
  }
}

/** Last day to file and pay: end of the second month after the period (e.g. 28.02 for the second semester). */
export function deadline(p: Period): string {
  const [year, month] = p.to.split('-').map(Number);
  const index = year * 12 + (month - 1) + 2;
  const y = Math.floor(index / 12);
  const m = (index % 12) + 1;
  return `${y}-${pad(m)}-${pad(lastDay(y, m))}`;
}

export function samePeriod(payment: VatPayment, p: Period): boolean {
  return payment.from === p.from && payment.to === p.to;
}

/** Payments covering part of the period without being its own: recording another one would be refused. */
export function overlapping(payments: VatPayment[], p: Period): VatPayment[] {
  return payments.filter((v) => !samePeriod(v, p) && v.from <= p.to && v.to >= p.from);
}

export type PeriodStatus = { text: string; tone: 'done' | 'open' | 'late' };

export function periodStatus(p: Period, payment: VatPayment | null, today: string): PeriodStatus {
  if (payment) {
    return { text: 'payée le ' + formatDate(payment.paidOn), tone: 'done' };
  }
  if (p.to >= today) {
    return { text: 'en cours', tone: 'open' };
  }
  return { text: deadline(p) < today ? 'en retard' : 'à payer', tone: 'late' };
}

/** VAT at the net tax rate, rounded to the centime. */
export function vatOn(amount: number, ratePercent: number): number {
  return fromCents(Math.round((toCents(amount) * ratePercent) / 100));
}

/** What the books say now compared with what the payment was computed on. */
export function booksChange(report: IncomeReport, payment: VatPayment): { revenue: number; vat: number } | null {
  const delta = toCents(report.total) - toCents(payment.revenue);
  if (delta === 0) {
    return null;
  }
  return { revenue: fromCents(delta), vat: vatOn(fromCents(delta), payment.vatRatePercent) };
}

export interface MonthFigure {
  key: string;
  label: string;
  total: number;
  vat: number;
  /** Share of the largest month, for the bar. */
  ratio: number;
}

/** Money received month by month within the period, with its VAT. */
export function monthlyIncome(report: IncomeReport, p: Period): MonthFigure[] {
  const sums = new Map<string, number>();
  let [y, m] = p.from.split('-').map(Number);
  const end = p.to.slice(0, 7);
  for (let guard = 0; guard < 240; guard++) {
    const key = `${y}-${pad(m)}`;
    if (key > end) break;
    sums.set(key, 0);
    m === 12 ? ((y += 1), (m = 1)) : (m += 1);
  }
  for (const line of [...(report.invoicePayments ?? []), ...(report.advances ?? [])]) {
    const key = (line.date ?? '').slice(0, 7);
    if (sums.has(key)) {
      sums.set(key, (sums.get(key) ?? 0) + toCents(line.amount));
    }
  }
  const max = Math.max(1, ...sums.values());
  const rate = report.vatRatePercent ?? 0;
  return [...sums.entries()].map(([key, cents]) => ({
    key,
    label: MONTHS[Number(key.slice(5)) - 1],
    total: fromCents(cents),
    vat: vatOn(fromCents(cents), rate),
    ratio: cents / max,
  }));
}

export function meansOf(line: IncomeLine): string {
  if (line.cash) return 'comptant';
  if (line.ticketNumber) return 'bulletin QR';
  return 'virement';
}

/** Figures to type in the AFC form. */
export function afcText(p: Period, report: IncomeReport): string {
  return [
    `Période : ${formatDate(p.from)} – ${formatDate(p.to)}`,
    `Chiffre d’affaires encaissé, TVA comprise : ${(report.total ?? 0).toFixed(2)}`,
    `Taux de la dette fiscale nette : ${report.vatRatePercent} %`,
    `TVA due : ${(report.vatDue ?? 0).toFixed(2)}`,
  ].join('\n');
}
