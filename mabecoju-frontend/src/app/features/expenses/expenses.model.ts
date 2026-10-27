import { OutgoingPayment, StandingOrder } from '../../api/generated';
import { fromCents, toCents } from '../../shared/money';

export type KindFilter = 'all' | 'BANK' | 'EXPENSE' | 'PERMANENT' | 'unpaid';

export const VAT_RATES = [0, 2.6, 3.8, 8.1];

const MONTHS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

/** "2026-10" → "octobre 2026" */
export function monthLabel(month: string): string {
  const [year, m] = month.split('-').map(Number);
  return `${MONTHS[m - 1]} ${year}`;
}

/** "d’octobre 2026", "de mars 2026": the month after "de", with the elision French requires. */
export function ofMonth(month: string): string {
  const label = monthLabel(month);
  return /^[aeiouéh]/i.test(label) ? 'd’' + label : 'de ' + label;
}

export function shiftMonth(month: string, delta: number): string {
  const [year, m] = month.split('-').map(Number);
  const index = year * 12 + (m - 1) + delta;
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`;
}

export const kindLabel = (kind: OutgoingPayment['kind']) =>
  kind === 'PERMANENT' ? 'Ordre permanent' : kind === 'EXPENSE' ? 'Frais' : 'Normal';

/** Type shown for a payment: the VAT paid to the AFC is a normal payment booked by the VAT screen. */
export const paymentLabel = (p: OutgoingPayment) => (p.vatPaymentId != null ? 'TVA' : kindLabel(p.kind));

/** VAT included in an amount: amount × rate / (100 + rate), as the API computes it. */
export function includedVat(amount: number, ratePercent: number): number {
  if (!ratePercent) {
    return 0;
  }
  return fromCents(Math.round((toCents(amount) * ratePercent) / (100 + ratePercent)));
}

/**
 * Payments of the month, by their date; an unpaid payment is shown whatever its date, so that nothing to pay is
 * forgotten. "unpaid" shows only those.
 */
export function visiblePayments(payments: OutgoingPayment[], month: string | null, filter: KindFilter): OutgoingPayment[] {
  return payments
    .filter((p) => (filter === 'unpaid' ? !p.paidOn : month === null || (p.date ?? '').startsWith(month)))
    .filter((p) => filter === 'all' || filter === 'unpaid' || p.kind === filter)
    .sort((a, b) => (b.date ?? '').localeCompare(a.date ?? '') || b.id - a.id);
}

export interface ExpenseFigures {
  monthTotal: number;
  monthCount: number;
  monthTax: number;
  unpaidTotal: number;
  unpaidCount: number;
  ordersTotal: number;
  ordersCount: number;
}

export function expenseFigures(payments: OutgoingPayment[], orders: StandingOrder[], month: string): ExpenseFigures {
  const inMonth = payments.filter((p) => (p.date ?? '').startsWith(month));
  const unpaid = payments.filter((p) => !p.paidOn);
  const active = orders.filter((o) => o.enabled);
  const sum = <T>(list: T[], amount: (x: T) => number | undefined) => fromCents(list.reduce((s, x) => s + toCents(amount(x)), 0));
  return {
    monthTotal: sum(inMonth, (p) => p.amount),
    monthCount: inMonth.length,
    monthTax: sum(inMonth, (p) => p.tax),
    unpaidTotal: sum(unpaid, (p) => p.amount),
    unpaidCount: unpaid.length,
    ordersTotal: sum(active, (o) => o.amount),
    ordersCount: active.length,
  };
}

/** The standing order a PERMANENT payment came from: same creditor and title. */
export function orderOf(payment: OutgoingPayment, orders: StandingOrder[]): StandingOrder | null {
  return orders.find((o) => (o.creditor ?? '') === (payment.creditor ?? '') && (o.title ?? '') === (payment.title ?? '')) ?? null;
}
