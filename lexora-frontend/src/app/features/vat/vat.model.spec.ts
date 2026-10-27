import { IncomeReport, VatPayment } from '../../api/generated';
import { booksChange, currentSemester, deadline, monthlyIncome, overlapping, periodLabel, periodStatus, semester, vatOn, wholeYear } from './vat.model';

const paidS1: VatPayment = { id: 1, from: '2026-01-01', to: '2026-06-30', revenue: 18000, vatRatePercent: 5.9, vatDue: 1062, amount: 1062, paidOn: '2026-08-28' };

describe('VAT periods', () => {
  it('follows the semesters of the AFC', () => {
    expect(currentSemester('2026-10-26')).toEqual({ kind: 'S2', from: '2026-07-01', to: '2026-12-31' });
    expect(currentSemester('2026-06-30').kind).toBe('S1');
    expect(periodLabel(semester(2026, 1))).toBe('1er semestre 2026');
    expect(periodLabel({ kind: 'custom', from: '2026-10-01', to: '2026-10-31' })).toBe('01.10.2026 – 31.10.2026');
  });

  it('gives two months after the period to file and pay', () => {
    expect(deadline(semester(2026, 1))).toBe('2026-08-31');
    expect(deadline(semester(2026, 2))).toBe('2027-02-28');
    expect(deadline(semester(2027, 2))).toBe('2028-02-29');
  });

  it('tells paid, running, due and late periods apart', () => {
    expect(periodStatus(semester(2026, 1), paidS1, '2026-10-26')).toEqual({ text: 'payée le 28.08.2026', tone: 'done' });
    expect(periodStatus(semester(2026, 2), null, '2026-10-26')).toEqual({ text: 'en cours', tone: 'open' });
    expect(periodStatus(semester(2026, 2), null, '2027-01-15')).toEqual({ text: 'à payer', tone: 'late' });
    expect(periodStatus(semester(2026, 2), null, '2027-03-01')).toEqual({ text: 'en retard', tone: 'late' });
  });

  it('finds the payments a period would overlap', () => {
    expect(overlapping([paidS1], wholeYear(2026)).map((p) => p.id)).toEqual([1]);
    expect(overlapping([paidS1], semester(2026, 1))).toEqual([]);
    expect(overlapping([paidS1], semester(2026, 2))).toEqual([]);
  });
});

describe('VAT figures', () => {
  const report: IncomeReport = {
    from: '2026-07-01', to: '2026-12-31', vatRatePercent: 5.9, total: 1386.4, vatDue: 81.8,
    invoicePayments: [{ date: '2026-10-02', amount: 324.3 }, { date: '2026-10-24', amount: 162.1 }],
    advances: [{ date: '2026-10-05', amount: 500 }, { date: '2026-08-20', amount: 400 }],
  };

  it('applies the net tax rate to the centime', () => {
    expect(vatOn(1386.4, 5.9)).toBe(81.8);
    expect(vatOn(300, 5.9)).toBe(17.7);
  });

  it('spreads the money received over the months of the period', () => {
    const months = monthlyIncome(report, semester(2026, 2));
    expect(months.map((m) => m.label)).toEqual(['juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']);
    expect(months.map((m) => m.total)).toEqual([0, 400, 0, 986.4, 0, 0]);
    expect(months[3]).toMatchObject({ vat: 58.2, ratio: 1 });
  });

  it('measures how the books changed since the VAT was paid', () => {
    expect(booksChange({ ...report, total: 18300 }, paidS1)).toEqual({ revenue: 300, vat: 17.7 });
    expect(booksChange({ ...report, total: 18000 }, paidS1)).toBeNull();
  });
});
