import { BankStatementAnalysis } from '../../api/generated';
import { effectOf, formatReference, initialDecisions, receipts, rows, summary } from './bank.model';

const analysis: BankStatementAnalysis = {
  accountIban: 'CH4431999123000889012',
  entries: [
    { entryReference: 'T-1', date: '2026-10-22', amount: 432.4, currency: 'CHF', reference: '21000…', debtorIban: 'CH93…', status: 'READY', problem: 'NONE',
      target: { kind: 'INVOICE', documentId: 1, folderId: 2, folderName: 'Keller', customerId: 3, customerName: 'Thomas Keller' }, remaining: 432.4 },
    { entryReference: 'T-2', date: '2026-10-22', amount: 100, currency: 'CHF', status: 'CHECK', problem: 'OVERPAID',
      target: { kind: 'INVOICE', documentId: 9, folderId: 8, folderName: 'Zbinden', customerId: 7, customerName: 'Marc' }, remaining: 80 },
    { entryReference: 'T-3', date: '2026-10-23', amount: 400, currency: 'CHF', status: 'CHECK', problem: 'SUGGESTED',
      target: { kind: 'ADVANCE_REQUEST', documentId: 1, folderId: 5, folderName: 'Favre', customerId: 7, customerName: 'Paul Favre' }, remaining: 400 },
    { entryReference: 'T-4', date: '2026-10-23', amount: 215.6, currency: 'CHF', status: 'UNMATCHED', problem: 'NOT_FOUND' },
    { entryReference: 'T-5', date: '2026-10-24', amount: 162.1, currency: 'CHF', status: 'DUPLICATE', problem: 'NONE' },
    { entryReference: 'T-6', date: '2026-10-24', amount: 50, currency: 'EUR', status: 'UNMATCHED', problem: 'CURRENCY' },
  ],
};

describe('bank import', () => {
  it('groups references as on the QR-bill', () => {
    expect(formatReference('000000010000000010000000038')).toBe('00 00000 10000 00001 00000 00038');
    expect(formatReference('RF18539007547034')).toBe('RF18 5390 0754 7034');
    expect(formatReference(undefined)).toBe('');
  });

  it('includes only the credits matched by reference at first', () => {
    expect(initialDecisions(analysis).map((d) => d.include)).toEqual([true, false, false, false, false, false]);
  });

  it('tells what each credit does to its document', () => {
    const r = rows(analysis, initialDecisions(analysis));
    expect(r.map((x) => x.effect)).toEqual(['solde le document', 'solde 80.00 · 20.00 en avance', 'solde le document', '', 'déjà enregistré', '']);
    expect(r.map((x) => x.documentRef)).toEqual(['F-1', 'F-9', 'P-1', null, null, null]);
    expect(r.map((x) => x.bookable)).toEqual([true, true, true, false, false, false]);
    expect(effectOf({ amount: 100, status: 'READY', target: {} }, null)).toBe('');
    expect(effectOf({ amount: 100, status: 'READY', target: {}, remaining: 250 }, null)).toBe('partiel · reste 150.00');
    expect(effectOf({ amount: 249.5, status: 'READY', target: {}, remaining: 250 }, null)).toBe('solde le document (écart −0.50)');
  });

  it('records the included credits, by match or by the document chosen by hand', () => {
    const decisions = initialDecisions(analysis);
    decisions[2] = { include: true, manual: null };
    decisions[3] = { include: true, manual: { kind: 'INVOICE', documentId: 3, folderId: 4, folderName: 'Bernasconi', remaining: 215.6 } };
    decisions[4] = { include: true, manual: null }; // a duplicate is never booked
    const list = rows(analysis, decisions);
    expect(receipts(list)).toEqual([
      { kind: 'INVOICE', documentId: 1, customerId: 3, date: '2026-10-22', amount: 432.4, iban: 'CH93…', reference: '21000…', entryReference: 'T-1' },
      { kind: 'ADVANCE_REQUEST', documentId: 1, customerId: 7, date: '2026-10-23', amount: 400, iban: undefined, reference: undefined, entryReference: 'T-3' },
      { kind: 'INVOICE', documentId: 3, customerId: undefined, date: '2026-10-23', amount: 215.6, iban: undefined, reference: undefined, entryReference: 'T-4' },
    ]);
    expect(summary(list)).toEqual({ count: 6, total: 1360.1, ready: 1, readyTotal: 432.4, toCheck: 4, duplicates: 1, included: 3, includedTotal: 1048 });
  });
});
