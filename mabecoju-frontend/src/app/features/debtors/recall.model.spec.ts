import { Customer } from '../../api/generated';
import { dueBy, fillTemplate, insertAt, isHtml, letterCopies, longDate, modelIndexFor, placeholderValues, recipientsAt, RecalledDocument } from './recall.model';

const nadia: Customer = { id: 6, title: 'Madame', firstName: 'Nadia', lastName: 'Favre', fullName: 'Nadia Favre', street: 'Rue des Parcs 30', zip: '2000', city: 'Neuchâtel' };
const paul: Customer = { id: 7, title: 'Monsieur', firstName: 'Paul', lastName: 'Favre', fullName: 'Paul Favre', street: 'Rue Louis-Favre 2', zip: '2017', city: 'Boudry' };
const request: RecalledDocument = {
  kind: 'request',
  doc: {
    id: 1, date: '2026-08-20', dueDate: '2026-09-19', delayDays: 30, amount: 800, remainingSum: 400,
    shares: [{ customerId: 6, amount: 400, paid: 400, remaining: 0 }, { customerId: 7, amount: 400, paid: 0, remaining: 400 }],
    recalls: [{ id: 1, date: '2026-10-12', degree: 1, amount: 800 }],
  },
};

describe('recall letter', () => {
  it('takes the date of the latest reminder whatever the order of the list', () => {
    const twice: RecalledDocument = {
      kind: 'invoice',
      doc: { id: 1, remainingSum: 432.4, recalls: [{ id: 2, degree: 2, date: '2026-10-26' }, { id: 1, degree: 1, date: '2026-10-12' }] },
    };
    expect(placeholderValues(twice, [nadia], null, 'Rappel', '2026-10-27')['lastRecallDate']).toBe('26.10.2026');
  });

  it('writes dates like the PDF', () => {
    expect(longDate('2026-10-04')).toBe('4 octobre 2026');
  });

  it('addresses the customers active on the document date', () => {
    const left = { ...paul, activeUntil: '2026-08-01' };
    const later = { ...paul, id: 8, activeSince: '2026-09-01' };
    expect(recipientsAt([nadia, left, later], '2026-08-20').map((c) => c.id)).toEqual([6]);
  });

  it('asks each customer for what he still owes', () => {
    expect(dueBy(nadia, [nadia, paul], request)).toBe(0);
    expect(dueBy(paul, [nadia, paul], request)).toBe(40000);
    expect(dueBy({ ...paul, billing: true }, [nadia, { ...paul, billing: true }], request)).toBe(40000);
    const alone: RecalledDocument = { kind: 'invoice', doc: { id: 2, remainingSum: 1081, shares: [] } };
    expect(dueBy(nadia, [nadia], alone)).toBe(108100);
  });

  it('fills the placeholders of the legacy templates', () => {
    const values = placeholderValues(request, [nadia, paul], { firstName: 'Demo', lastName: 'User', shortcut: 'dmo' }, 'Rappel', '2026-10-26');
    const text = '$title_1 et ${title_2}, notre envoi n° $refNo du $invoiceDate (rappel du $lastRecallDate) : frs. $amount_2 par $issuerName.$unknown';
    expect(fillTemplate(text, values)).toBe(
      'Madame et Monsieur, notre envoi n° 1 du 20 août 2026 (rappel du 12.10.2026) : frs. 400.00 par Demo User.',
    );
    expect(values['date']).toBe('26 octobre 2026');
    expect(values['amount']).toBe('0.00');
  });

  it('prints one copy per customer, the last one moving to the front, or a merged letter', () => {
    const third: Customer = { ...paul, id: 9, firstName: 'Léa', fullName: 'Léa Favre' };
    const copies = letterCopies([nadia, paul, third], request, false);
    expect(copies.map((c) => c.addresses.map((a) => a[1]))).toEqual([
      ['Nadia Favre', 'Paul Favre', 'Léa Favre'],
      ['Léa Favre', 'Nadia Favre', 'Paul Favre'],
      ['Paul Favre', 'Léa Favre', 'Nadia Favre'],
    ]);
    const merged = letterCopies([nadia, paul], request, true);
    expect(merged).toEqual([
      { recipient: null, addresses: [['Madame et Monsieur', 'Nadia et Paul Favre', 'Rue des Parcs 30', '2000 Neuchâtel']], slipCents: 40000 },
    ]);
  });

  it('proposes the template matching the reminder number', () => {
    expect(modelIndexFor(1, 3)).toBe(0);
    expect(modelIndexFor(2, 3)).toBe(1);
    expect(modelIndexFor(5, 3)).toBe(2);
    expect(modelIndexFor(1, 0)).toBe(-1);
  });

  it('inserts a placeholder at the cursor', () => {
    expect(insertAt('Bonjour,', '$title_1', 7)).toEqual({ text: 'Bonjour $title_1,', cursor: 16 });
    expect(insertAt('frs. ', '$amount_1', 5)).toEqual({ text: 'frs. $amount_1', cursor: 14 });
  });

  it('tells HTML templates from plain text', () => {
    expect(isHtml('<p>Madame,</p>')).toBe(true);
    expect(isHtml('Montant < 100 et > 50')).toBe(false);
  });
});
