import { CustomerStatement } from '../../api/generated';
import { allocate } from './payment.model';

const statement: CustomerStatement = {
  invoices: [
    { documentId: 9, date: '2026-09-20', dueDate: '2026-10-20', remaining: 112.15 },
    { documentId: 3, date: '2026-08-01', dueDate: '2026-08-31', remaining: 200 },
  ],
  requests: [{ documentId: 1, date: '2026-08-20', dueDate: '2026-09-19', remaining: 400 }],
  totalRemaining: 712.15,
};

describe('allocate', () => {
  it('pays the oldest due documents first', () => {
    const plan = allocate(50000, statement);
    expect(plan.allocations.map((a) => [a.reference, a.cents, a.settles])).toEqual([
      ['F-3', 20000, true],
      ['P-1', 30000, false],
    ]);
    expect(plan.surplusCents).toBe(0);
  });

  it('keeps what exceeds the open documents as a spontaneous advance', () => {
    const plan = allocate(80000, statement);
    expect(plan.allocations.map((a) => a.cents)).toEqual([20000, 40000, 11215]);
    expect(plan.allocations.every((a) => a.settles)).toBe(true);
    expect(plan.surplusCents).toBe(8785);
  });

  it('turns the whole payment into an advance when nothing is open', () => {
    expect(allocate(30000, { invoices: [], requests: [] })).toEqual({ allocations: [], surplusCents: 30000 });
    expect(allocate(30000, null).surplusCents).toBe(30000);
  });
});
