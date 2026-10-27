import { BankEntry, BankReceiptInput, BankStatementAnalysis } from '../../api/generated';
import { formatChf } from '../../shared/format';
import { fromCents, toCents } from '../../shared/money';
import { texts } from '../../i18n/i18n';
import { invoiceRef, requestRef } from '../folder/timeline.model';

/** A document chosen by hand for a credit without match. */
export interface ManualChoice {
  kind: 'INVOICE' | 'ADVANCE_REQUEST';
  documentId: number;
  folderId: number;
  folderName: string;
  remaining: number;
}

export interface Decision {
  /** Included in the receipts to record. */
  include: boolean;
  manual: ManualChoice | null;
}

export interface Row {
  index: number;
  entry: BankEntry;
  decision: Decision;
  /** The document the receipt goes to (the match, or the manual choice). */
  documentRef: string | null;
  effect: string;
  /** Can be included at all. */
  bookable: boolean;
}

export const docRef = (kind: string | undefined, id: number | undefined) =>
  kind === 'ADVANCE_REQUEST' ? requestRef(id) : invoiceRef(id);

/** By default the credits matched by reference are included; suggestions and overpayments wait for a confirmation. */
export function initialDecisions(analysis: BankStatementAnalysis): Decision[] {
  return (analysis.entries ?? []).map((e) => ({ include: e.status === 'READY', manual: null }));
}

export function rows(analysis: BankStatementAnalysis, decisions: Decision[]): Row[] {
  return (analysis.entries ?? []).map((entry, index) => {
    const decision = decisions[index] ?? { include: false, manual: null };
    const manual = decision.manual;
    const bookable = entry.status !== 'DUPLICATE' && entry.problem !== 'CURRENCY' && (!!entry.target || !!manual) && !(entry.problem === 'DOCUMENT_PAID' && !manual);
    const documentRef = manual ? docRef(manual.kind, manual.documentId) : entry.target ? docRef(entry.target.kind, entry.target.documentId) : null;
    return { index, entry, decision, documentRef, effect: effectOf(entry, manual), bookable };
  });
}

/** What recording the credit does to its document. */
export function effectOf(entry: BankEntry, manual: ManualChoice | null): string {
  const t = texts().bank.effect;
  if (entry.status === 'DUPLICATE') {
    return t.duplicate;
  }
  const remaining = manual ? manual.remaining : entry.remaining;
  if (remaining === undefined || remaining === null || (!entry.target && !manual)) {
    return '';
  }
  const amount = toCents(entry.amount);
  const owed = toCents(remaining);
  if (owed <= 0) {
    return t.paid;
  }
  if (amount > owed + 100) {
    return t.surplus(formatChf(fromCents(owed)), formatChf(fromCents(amount - owed)));
  }
  if (amount >= owed - 100) {
    return amount === owed ? t.settles : t.settlesGap(formatChf(fromCents(amount - owed)));
  }
  return t.partial(formatChf(fromCents(owed - amount)));
}

export function receipts(list: Row[]): BankReceiptInput[] {
  return list
    .filter((r) => r.decision.include && r.bookable)
    .map((r) => {
      const manual = r.decision.manual;
      const target = r.entry.target;
      return {
        kind: manual ? manual.kind : target!.kind!,
        documentId: manual ? manual.documentId : target!.documentId!,
        // a document chosen by hand: the API takes the customer who owes that amount
        customerId: manual ? undefined : target!.customerId,
        date: r.entry.date!,
        amount: r.entry.amount!,
        iban: r.entry.debtorIban,
        reference: r.entry.reference,
        entryReference: r.entry.entryReference,
      };
    });
}

export interface Summary {
  count: number;
  total: number;
  ready: number;
  readyTotal: number;
  toCheck: number;
  duplicates: number;
  included: number;
  includedTotal: number;
}

export function summary(list: Row[]): Summary {
  const sum = (xs: Row[]) => fromCents(xs.reduce((s, r) => s + toCents(r.entry.amount), 0));
  const ready = list.filter((r) => r.entry.status === 'READY');
  const included = list.filter((r) => r.decision.include && r.bookable);
  return {
    count: list.length,
    total: sum(list),
    ready: ready.length,
    readyTotal: sum(ready),
    toCheck: list.filter((r) => r.entry.status === 'CHECK' || r.entry.status === 'UNMATCHED').length,
    duplicates: list.filter((r) => r.entry.status === 'DUPLICATE').length,
    included: included.length,
    includedTotal: sum(included),
  };
}

/** References grouped as printed on QR-bills: QR in 2 + 5×5 digits, creditor references by 4. */
export function formatReference(reference: string | undefined): string {
  if (!reference) {
    return '';
  }
  const r = reference.replace(/\s+/g, '');
  if (/^\d{27}$/.test(r)) {
    return r.slice(0, 2) + ' ' + r.slice(2).replace(/(\d{5})(?=\d)/g, '$1 ');
  }
  return r.replace(/(.{4})(?=.)/g, '$1 ');
}
