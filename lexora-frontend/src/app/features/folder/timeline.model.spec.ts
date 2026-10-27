import { buildTimeline, documentState, FolderData, folderFigures, groupByMonth } from './timeline.model';

const today = '2026-10-26';

function favre(): FolderData {
  return {
    folder: { id: 5, number: 5, name: 'Favre', type: 'INVOICING', openingDate: '2026-08-15', handledBy: 'dmo' },
    customers: [
      { id: 6, firstName: 'Nadia', lastName: 'Favre', fullName: 'Nadia Favre' },
      { id: 7, firstName: 'Paul', lastName: 'Favre', fullName: 'Paul Favre' },
    ],
    honoraries: [
      { id: 1, date: '2026-09-08', description: 'Séance de médiation', minutes: 90, hourlyRate: 200, amount: 300, invoiceId: 9 },
      { id: 2, date: '2026-10-06', description: 'Séance de médiation', minutes: 60, hourlyRate: 200, amount: 200, expenses: [{ amount: 12.5 }] },
    ],
    invoices: [
      {
        id: 9, date: '2026-09-20', dueDate: '2026-10-20', honorariesSum: 300, advancedSum: 100, tax: 24.3, amount: 224.3,
        paid: false, remainingSum: 124.3,
        shares: [{ customerId: 6, amount: 112.15, paid: 100, remaining: 12.15 }, { customerId: 7, amount: 112.15, paid: 0, remaining: 112.15 }],
        payments: [{ id: 3, customerId: 6, date: '2026-10-02', amount: 100, cash: true }],
        recalls: [{ id: 4, date: '2026-10-25', degree: 1, amount: 124.3 }],
      },
    ],
    requests: [
      {
        id: 1, date: '2026-08-20', dueDate: '2026-09-19', amount: 800, paid: false, remainingSum: 400,
        shares: [{ customerId: 6, amount: 400, paid: 400, remaining: 0 }, { customerId: 7, amount: 400, paid: 0, remaining: 400 }],
      },
    ],
    advances: [
      { id: 1, requestId: 1, customerId: 6, date: '2026-10-21', amount: 400, iban: 'CH93…' },
      { id: 2, customerId: 7, date: '2026-09-01', amount: 100, cash: true, invoiceId: 9 },
    ],
  };
}

describe('buildTimeline', () => {
  const items = buildTimeline(favre(), today);

  it('lists every event, the most recent first, the payments after the document of the same day', () => {
    expect(items.map((i) => i.key)).toEqual(['fr4', 'a1', 'h2', 'fp3', 'f9', 'h1', 'a2', 'p1', 'opening']);
  });

  it('describes sessions with their duration, rate and expenses', () => {
    const open = items.find((i) => i.key === 'h2')!;
    expect(open.sub).toBe('60 min · 200.00/h · frais 12.50');
    expect(open.status).toBe('à facturer');
    expect(open.effects[0].text).toBe('+200.00 à facturer');
    expect(items.find((i) => i.key === 'h1')!.status).toBe('facturée');
  });

  it('shows the shares of the documents and their state', () => {
    const invoice = items.find((i) => i.key === 'f9')!;
    expect(invoice.title).toBe('Facture F-9');
    expect(invoice.sub).toBe('Nadia 112.15 · Paul 112.15 · échéance 20.10.2026');
    expect(invoice.status).toBe('partiellement payée');
    expect(invoice.tone).toBe('late');
    expect(items.find((i) => i.key === 'p1')!.effects[0].text).toBe('800.00 demandés à Nadia et Paul');
  });

  it('tells how advances were paid and whether they are still available', () => {
    expect(items.find((i) => i.key === 'a1')!.sub).toBe('Virement · répond à P-1');
    expect(items.find((i) => i.key === 'a1')!.status).toBe('disponible');
    expect(items.find((i) => i.key === 'a2')!.sub).toBe('Comptant · spontanée');
    expect(items.find((i) => i.key === 'a2')!.status).toBe('déduite');
  });
});

describe('documentState', () => {
  it('distinguishes paid, partly paid, overdue and open documents', () => {
    expect(documentState({ paid: true, paidOn: '2026-09-19', amount: 600 }, today)).toEqual({ status: 'payée le 19.09', tone: 'done' });
    expect(documentState({ amount: 600, remainingSum: 600, dueDate: '2026-11-01' }, today)).toEqual({ status: 'ouverte', tone: 'open' });
    expect(documentState({ amount: 600, remainingSum: 600, dueDate: '2026-10-01' }, today)).toEqual({ status: 'échue', tone: 'late' });
    expect(documentState({ amount: 600, remainingSum: 300, dueDate: '2026-11-01' }, today).status).toBe('partiellement payée');
  });
});

describe('groupByMonth', () => {
  it('sums the sessions and what was cashed in each month', () => {
    const months = groupByMonth(buildTimeline(favre(), today));
    expect(months.map((m) => m.label)).toEqual(['octobre 2026', 'septembre 2026', 'août 2026']);
    expect(months[0].summary).toBe('1 séance · 1 h 00 · 200.00 d’honoraires · encaissé 500.00');
    expect(months[1].summary).toBe('1 séance · 1 h 30 · 300.00 d’honoraires · encaissé 100.00');
    expect(months[2].summary).toBe('');
  });
});

describe('folderFigures', () => {
  it('computes what is left to invoice, the available advances and what each customer owes', () => {
    const figures = folderFigures(favre());
    expect(figures.toInvoice).toBe(200);
    expect(figures.openSessions).toBe(1);
    expect(figures.available).toBe(400);
    expect(figures.uncovered).toBe(0);
    expect(figures.dues.map((d) => [d.customer.firstName, d.due])).toEqual([['Nadia', 12.15], ['Paul', 512.15]]);
    expect(figures.dueTotal).toBe(524.3);
  });
});
