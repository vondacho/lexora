import { Customer, Share } from '../../api/generated';
import { fromCents, splitCents } from '../../shared/money';

export interface ShareLine {
  customer: Customer;
  cents: number;
}

/**
 * Who pays what: a billing customer pays everything; otherwise the amount is split by the weights
 * (equal parts by default), each share rounded to 5 centimes.
 */
export function distribute(totalCents: number, customers: Customer[], weights?: number[]): ShareLine[] {
  if (customers.length === 0) {
    return [];
  }
  const billing = customers.findIndex((c) => c.billing);
  const used = billing >= 0 ? customers.map((_, i) => (i === billing ? 1 : 0)) : weights ?? customers.map(() => 1);
  const amounts = splitCents(Math.max(0, totalCents), used);
  return customers.map((customer, i) => ({ customer, cents: amounts[i] }));
}

export function toShares(lines: ShareLine[]): Share[] {
  return lines.map((l) => ({ customerId: l.customer.id, amount: fromCents(l.cents) }));
}
