import { formatChf, formatDate } from './format';

describe('formatChf', () => {
  it('groups thousands with an apostrophe and keeps two decimals', () => {
    expect(formatChf(1230.4)).toBe("1'230.40");
    expect(formatChf(1234567)).toBe("1'234'567.00");
    expect(formatChf(0)).toBe('0.00');
  });

  it('writes negative amounts with a minus sign', () => {
    expect(formatChf(-708.3)).toBe('−708.30');
    expect(formatChf(-0.001)).toBe('0.00');
  });

  it('shows a dash when the amount is unknown', () => {
    expect(formatChf(undefined)).toBe('–');
  });
});

describe('formatDate', () => {
  it('writes dates the Swiss way', () => {
    expect(formatDate('2026-10-24')).toBe('24.10.2026');
    expect(formatDate('2026-10-24', false)).toBe('24.10');
    expect(formatDate(undefined)).toBe('');
  });
});
