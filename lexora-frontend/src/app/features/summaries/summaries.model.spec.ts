import { YearOverview } from '../../api/generated';
import { change, yearSummary } from './summaries.model';

const years: YearOverview[] = [
  { year: 2026, months: [
    { month: 'SEPTEMBER', advances: 500, invoicePayments: 2157.05, incomes: 2657.05, expenses: 2800, balance: -142.95 },
    { month: 'OCTOBER', advances: 1000, invoicePayments: 4151, incomes: 5151, expenses: 4012.1, balance: 1138.9 },
  ] },
  { year: 2025, months: [
    { month: 'OCTOBER', advances: 0, invoicePayments: 2000, incomes: 2000, expenses: 100, balance: 1900 },
    { month: 'DECEMBER', advances: 0, invoicePayments: 9000, incomes: 9000, expenses: 0, balance: 9000 },
  ] },
];

describe('yearSummary', () => {
  it('lists the twelve months with bars scaled on the largest amount', () => {
    const s = yearSummary(years, 2026, '2026-10-26');
    expect(s.rows).toHaveLength(12);
    expect(s.rows[9]).toMatchObject({ label: 'octobre', incomes: 5151, expenses: 4012.1, incomeRatio: 1, future: false });
    expect(s.rows[10].future).toBe(true);
  });

  it('totals the year and averages over the months elapsed', () => {
    const s = yearSummary(years, 2026, '2026-10-26');
    expect(s.total).toEqual({ advances: 1500, invoices: 6308.05, incomes: 7808.05, expenses: 6812.1, balance: 995.95 });
    expect(s.monthsCounted).toBe(10);
    expect(s.average.incomes).toBe(780.81);
  });

  it('compares with the same months of the year before', () => {
    const s = yearSummary(years, 2026, '2026-10-26');
    expect(s.previousIncomes).toBe(2000); // December 2025 is not counted yet
    expect(change(7808.05, s.previousIncomes)).toBe(290.4);
    expect(yearSummary(years, 2025, '2026-10-26').previousIncomes).toBeNull();
    expect(yearSummary(years, 2025, '2026-10-26').monthsCounted).toBe(12);
  });
});
