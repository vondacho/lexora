import { formatMinutes, round5Cents, splitCents, toCents, todayIso, vatCents } from './money';

describe('money', () => {
  it('converts amounts to centimes without drift', () => {
    expect(toCents(1119.99)).toBe(111999);
    expect(toCents(0.1 + 0.2)).toBe(30);
  });

  it('truncates the VAT to 10 centimes', () => {
    expect(vatCents(40000, 8.1)).toBe(3240);
    expect(vatCents(15000, 8.1)).toBe(1210); // 12.15 → 12.10
    expect(vatCents(146668, 8.1)).toBe(11880); // 118.80…
  });

  it('rounds shares to 5 centimes', () => {
    expect(round5Cents(21622)).toBe(21620);
    expect(round5Cents(21623)).toBe(21625);
  });

  it('splits an amount so that the shares add up', () => {
    expect(splitCents(43240, [1, 1])).toEqual([21620, 21620]);
    expect(splitCents(100000, [1, 1, 1])).toEqual([33335, 33335, 33330]);
    expect(splitCents(43240, [7, 3])).toEqual([30270, 12970]);
    expect(splitCents(43240, [1, 0])).toEqual([43240, 0]);
  });

  it('formats durations and dates', () => {
    expect(formatMinutes(90)).toBe('1 h 30');
    expect(formatMinutes(330)).toBe('5 h 30');
    expect(todayIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
