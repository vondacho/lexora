import { OutgoingPayment, StandingOrder } from '../../api/generated';
import { expenseFigures, includedVat, monthLabel, ofMonth, orderOf, paymentLabel, shiftMonth, visiblePayments } from './expenses.model';

const payments: OutgoingPayment[] = [
  { id: 1, kind: 'PERMANENT', date: '2026-10-01', creditor: 'Gérance du Lac SA', title: 'Loyer du bureau', amount: 2154, tax: 0, paidOn: '2026-10-01' },
  { id: 2, kind: 'EXPENSE', date: '2026-10-20', creditor: 'Papeterie Payot', title: 'Classeurs', amount: 54.1, tax: 4.05, paidOn: '2026-10-20' },
  { id: 3, kind: 'BANK', date: '2026-10-24', creditor: 'Fédération', title: 'Cotisation', amount: 350, tax: 0 },
  { id: 4, kind: 'BANK', date: '2026-09-16', creditor: 'Swisscom', title: 'Téléphone', amount: 129, tax: 9.67, paidOn: '2026-09-16' },
  { id: 5, kind: 'BANK', date: '2026-08-30', creditor: 'Ancien', title: 'Facture oubliée', amount: 80, tax: 0 },
];
const orders: StandingOrder[] = [
  { id: 1, creditor: 'Gérance du Lac SA', title: 'Loyer du bureau', amount: 2154, dayOfMonth: 1, enabled: true },
  { id: 2, creditor: 'Assurances du Jura', title: 'RC', amount: 86.4, dayOfMonth: 5, enabled: true },
  { id: 3, creditor: 'Ancien bail', title: 'Parking', amount: 120, dayOfMonth: 1, enabled: false },
];

describe('expenses', () => {
  it('names and shifts months', () => {
    expect(monthLabel('2026-10')).toBe('octobre 2026');
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
    expect(ofMonth('2026-10')).toBe('d’octobre 2026');
    expect(ofMonth('2026-08')).toBe('d’août 2026');
    expect(ofMonth('2026-03')).toBe('de mars 2026');
    expect(shiftMonth('2026-12', 1)).toBe('2027-01');
  });

  it('computes the VAT included in an amount', () => {
    expect(includedVat(54.1, 8.1)).toBe(4.05);
    expect(includedVat(129, 8.1)).toBe(9.67);
    expect(includedVat(100, 0)).toBe(0);
  });

  it('shows the payments of the month, most recent first, or only those left to pay', () => {
    expect(visiblePayments(payments, '2026-10', 'all').map((p) => p.id)).toEqual([3, 2, 1]);
    expect(visiblePayments(payments, '2026-10', 'BANK').map((p) => p.id)).toEqual([3]);
    expect(visiblePayments(payments, '2026-10', 'unpaid').map((p) => p.id)).toEqual([3, 5]);
    expect(visiblePayments(payments, null, 'PERMANENT').map((p) => p.id)).toEqual([1]);
  });

  it('sums the month, what is left to pay and the active orders', () => {
    expect(expenseFigures(payments, orders, '2026-10')).toEqual({
      monthTotal: 2558.1, monthCount: 3, monthTax: 4.05, unpaidTotal: 430, unpaidCount: 2, ordersTotal: 2240.4, ordersCount: 2,
    });
  });

  it('labels the VAT paid to the AFC apart from the other payments', () => {
    expect(paymentLabel({ id: 9, kind: 'BANK', vatPaymentId: 3 })).toBe('TVA');
    expect(paymentLabel(payments[2])).toBe('Normal');
    expect(paymentLabel(payments[0])).toBe('Ordre permanent');
  });

  it('finds the order behind a standing order payment', () => {
    expect(orderOf(payments[0], orders)?.id).toBe(1);
    expect(orderOf(payments[1], orders)).toBeNull();
  });
});
