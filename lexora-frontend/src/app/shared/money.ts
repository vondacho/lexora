/**
 * Amounts are computed in centimes (integers) to avoid floating point drift, with the rounding rules of the API:
 * VAT truncated to 10 centimes, shares rounded to 5 centimes.
 */
const EPSILON = 1e-6;

export function toCents(amount: number | null | undefined): number {
  return Math.round((amount ?? 0) * 100);
}

export function fromCents(cents: number): number {
  return cents / 100;
}

/** VAT of a subtotal, truncated to 10 centimes. */
export function vatCents(subtotalCents: number, ratePercent: number): number {
  return Math.trunc((subtotalCents * ratePercent) / 1000 + EPSILON) * 10;
}

/** Nearest 5 centimes. */
export function round5Cents(cents: number): number {
  return Math.round(cents / 5) * 5;
}

/**
 * Splits an amount between customers with the given weights, each share rounded to 5 centimes;
 * the last share takes what is left so that the shares add up to the amount (rounded too).
 */
export function splitCents(totalCents: number, weights: number[]): number[] {
  const sum = weights.reduce((a, b) => a + b, 0);
  if (weights.length === 0 || sum <= 0) {
    return weights.map(() => 0);
  }
  const shares: number[] = [];
  let allotted = 0;
  weights.forEach((weight, index) => {
    if (index === weights.length - 1) {
      shares.push(round5Cents(totalCents - allotted));
    } else {
      const share = round5Cents((totalCents * weight) / sum);
      shares.push(share);
      allotted += share;
    }
  });
  return shares;
}

/** Hours and minutes: 90 → "1 h 30". */
export function formatMinutes(minutes: number): string {
  return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}`;
}

/** Today in the local time zone, as an ISO date (2026-10-26). */
export function todayIso(now = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}
