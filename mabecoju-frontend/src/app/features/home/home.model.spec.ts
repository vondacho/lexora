import { DebtorsOverview, YearOverview } from '../../api/generated';
import { debtsToRecall, foldersToBill, monthSummary } from './home.model';

describe('debtsToRecall', () => {
  const overview: DebtorsOverview = {
    openInvoices: [
      { documentId: 2214, folderId: 1, folderName: 'Keller', daysOverdue: 62, remaining: 450, recallCount: 1, lastRecallDate: '2026-10-12' },
      { documentId: 2230, folderId: 2, folderName: 'Pittet', daysOverdue: -5, remaining: 512.8, recallCount: 0 },
      { documentId: 2231, folderId: 3, folderName: 'Payé', daysOverdue: 40, remaining: 0, recallCount: 0 },
    ],
    openRequests: [{ documentId: 371, folderId: 4, folderName: 'Favre', daysOverdue: 41, remaining: 800, recallCount: 0 }],
  };

  it('keeps the overdue debts with something left to pay, the longest overdue first', () => {
    const result = debtsToRecall(overview);
    expect(result.items.map((d) => d.reference)).toEqual(['F-2214', 'P-371']);
    expect(result.count).toBe(2);
    expect(result.total).toBe(1250);
  });

  it('proposes the next reminder', () => {
    const [keller, favre] = debtsToRecall(overview).items;
    expect(keller.nextRecall).toBe(2);
    expect(favre.nextRecall).toBe(1);
  });

  it('keeps only the top ones but counts them all', () => {
    const result = debtsToRecall(overview, 1);
    expect(result.items.length).toBe(1);
    expect(result.count).toBe(2);
    expect(result.total).toBe(1250);
  });
});

describe('foldersToBill', () => {
  it('orders the folders by the amount advances do not cover', () => {
    const result = foldersToBill({
      foldersToInvoice: [
        { folderId: 1, folderName: 'Dupont', toBeInvoiced: 912.5, available: 600, uncovered: 312.5 },
        { folderId: 2, folderName: 'Rochat', toBeInvoiced: 1840, available: 0, uncovered: 1840 },
        { folderId: 3, folderName: 'Vide', toBeInvoiced: 0, available: 200, uncovered: 0 },
      ],
    });
    expect(result.items.map((f) => f.folderName)).toEqual(['Rochat', 'Dupont']);
    expect(result.total).toBe(2152.5);
  });
});

describe('monthSummary', () => {
  const years: YearOverview[] = [
    { year: 2026, months: [
      { month: 'SEPTEMBER', advances: 500, invoicePayments: 2157.05, incomes: 2657.05, expenses: 2800, balance: -142.95 },
      { month: 'OCTOBER', advances: 1000, invoicePayments: 4151, incomes: 5151, expenses: 4012.1, balance: 1138.9 },
    ] },
    { year: 2025, months: [{ month: 'DECEMBER', advances: 0, invoicePayments: 100, incomes: 100, expenses: 50, balance: 50 }] },
  ];

  it('gives the current and previous month with the VAT on the incomes', () => {
    const summary = monthSummary(years, new Date(2026, 9, 26), 5.9);
    expect(summary.figures.incomes).toBe(5151);
    expect(summary.previous.incomes).toBe(2657.05);
    expect(summary.vatDue).toBe(303.91);
  });

  it('looks for the previous month in the year before in January', () => {
    const summary = monthSummary(years, new Date(2026, 0, 10), null);
    expect(summary.figures.incomes).toBe(0);
    expect(summary.previous.incomes).toBe(100);
    expect(summary.vatDue).toBeNull();
  });
});
