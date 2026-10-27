import { AdvanceRequest, Customer, Invoice, User } from '../../api/generated';
import { toCents } from '../../shared/money';
import { latestRecall } from '../../shared/recalls';

/** Invoice or advance request being recalled, as the letter needs it. */
export interface RecalledDocument {
  kind: 'invoice' | 'request';
  doc: Invoice | AdvanceRequest;
}

const MAX_RECIPIENTS = 3;
const MONTHS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

/** "24 octobre 2026", as the PDF writes dates in letters. */
export function longDate(iso: string | undefined): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-').map(Number);
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

function shortDate(iso: string | undefined): string {
  return iso ? iso.split('-').reverse().join('.') : '';
}

/** The customers active on the document date, at most three, as the API addresses its letters. */
export function recipientsAt(customers: Customer[], date: string | undefined): Customer[] {
  const day = date ?? '';
  return customers
    .filter((c) => (!c.activeSince || c.activeSince <= day) && (!c.activeUntil || c.activeUntil >= day))
    .slice(0, MAX_RECIPIENTS);
}

/** What a recipient is asked for in a reminder: what he still owes (everything for a billing customer or a lone one). */
export function dueBy(customer: Customer, recipients: Customer[], recalled: RecalledDocument): number {
  const doc = recalled.doc;
  const shares = doc.shares ?? [];
  const remaining = toCents(doc.remainingSum);
  if (customer.billing || (recipients.length === 1 && shares.length === 0)) {
    return remaining;
  }
  if (recipients.length > 1 && recipients.some((r) => r.billing)) {
    return 0;
  }
  return toCents(shares.find((s) => s.customerId === customer.id)?.remaining);
}

const plain = (cents: number) => (cents / 100).toFixed(2);

/** Values of the template placeholders for a reminder printed today. */
export function placeholderValues(
  recalled: RecalledDocument,
  recipients: Customer[],
  issuer: User | null,
  modelTitle: string,
  today: string,
): Record<string, string> {
  const doc = recalled.doc;
  const recalls = doc.recalls ?? [];
  const values: Record<string, string> = {
    issuerName: [issuer?.firstName, issuer?.lastName].filter(Boolean).join(' '),
    issuerShortcut: issuer?.shortcut ?? '',
    refNo: String(doc.id),
    date: longDate(today),
    delay: String(doc.delayDays ?? ''),
    dueDate: longDate(doc.dueDate),
    modelTitle,
    invoiceDate: longDate(doc.date),
    // the reminder being created follows the last one sent
    lastRecallDate: shortDate(latestRecall(recalls)?.date),
  };
  for (let i = 0; i < MAX_RECIPIENTS; i++) {
    const n = '_' + (i + 1);
    const c = recipients[i];
    values['title' + n] = c?.title ?? '';
    values['firstName' + n] = c?.firstName ?? '';
    values['lastName' + n] = c?.lastName ?? '';
    values['streetNo' + n] = c?.street ?? '';
    values['zip' + n] = c?.zip ?? '';
    values['city' + n] = c?.city ?? '';
    values['amount' + n] = plain(c ? dueBy(c, recipients, recalled) : 0);
  }
  const billing = recipients.find((c) => c.billing);
  values['amount'] = billing ? plain(dueBy(billing, recipients, recalled)) : values['amount_1'];
  return values;
}

const PLACEHOLDER = /\$\{([A-Za-z_][A-Za-z0-9_]*)}|\$([A-Za-z_][A-Za-z0-9_]*)/g;

/** Replaces $name and ${name}; unknown placeholders print nothing, as in the PDF. */
export function fillTemplate(text: string, values: Record<string, string>): string {
  return text.replace(PLACEHOLDER, (_m, braced: string | undefined, bare: string | undefined) => values[braced ?? bare ?? ''] ?? '');
}

/** Templates written with the HTML editor; the older ones are plain text. */
export function isHtml(text: string): boolean {
  return /<\/?(p|br|div|strong|em|b|i|u|ul|ol|li|span|h[1-6])\b[^>]*>/i.test(text);
}

/** The n-th reminder uses the n-th reminder template, the last one beyond. */
export function modelIndexFor(degree: number, count: number): number {
  return count === 0 ? -1 : Math.min(Math.max(degree, 1), count) - 1;
}

/** Inserts a placeholder at the cursor, with spaces around it when needed. */
export function insertAt(text: string, token: string, start: number, end = start): { text: string; cursor: number } {
  const before = text.slice(0, start);
  const after = text.slice(end);
  const left = before && !/\s$/.test(before) ? ' ' : '';
  const right = after && !/^[\s.,;:!?)]/.test(after) ? ' ' : '';
  const inserted = left + token + right;
  return { text: before + inserted + after, cursor: before.length + left.length + token.length };
}

export interface LetterCopy {
  /** The recipient of this copy, or null for a single letter to all. */
  recipient: Customer | null;
  addresses: string[][];
  /** Amount of the QR-bill at the end of the copy, in centimes (none when 0). */
  slipCents: number;
}

/** One copy per recipient (his address first), or a single letter with both names. */
export function letterCopies(recipients: Customer[], recalled: RecalledDocument, merge: boolean): LetterCopy[] {
  const address = (c: Customer) =>
    [c.title ?? '', c.fullName || [c.firstName, c.lastName].filter(Boolean).join(' '), c.street ?? '', [c.zip, c.city].filter(Boolean).join(' ')].filter(Boolean);
  if (merge && recipients.length > 1) {
    const [a, b] = recipients;
    const merged = [
      [a.title, b.title].filter(Boolean).join(' et '),
      [[a.firstName, b.firstName].filter(Boolean).join(' et '), a.lastName].filter(Boolean).join(' '),
      a.street ?? '',
      [a.zip, a.city].filter(Boolean).join(' '),
    ].filter(Boolean);
    return [{ recipient: null, addresses: [merged], slipCents: toCents(recalled.doc.remainingSum) }];
  }
  // Collections.rotate of the API: at each copy the last recipient moves to the front (A B C, then C A B, then B C A)
  const n = recipients.length;
  return recipients.map((_, shift) => {
    const rotated = recipients.map((_c, i) => recipients[(i - shift + n) % n]);
    const recipient = rotated[0];
    return { recipient, addresses: rotated.map(address), slipCents: dueBy(recipient, recipients, recalled) };
  });
}
