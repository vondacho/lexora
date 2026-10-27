import { Advance, AdvanceRequest, Customer, CustomerShare, Folder, Honorary, Invoice, InvoicePayment } from '../../api/generated';
import { formatChf, formatDate } from '../../shared/format';
import { formatMinutes, fromCents, toCents } from '../../shared/money';

/** Everything the folder screen shows, as loaded from the API. */
export interface FolderData {
  folder: Folder;
  customers: Customer[];
  honoraries: Honorary[];
  invoices: Invoice[];
  requests: AdvanceRequest[];
  advances: Advance[];
}

export type Tone = 'open' | 'done' | 'late' | 'neutral';
export type Category = 'session' | 'billing' | 'cash';
export type EffectTone = 'up' | 'down' | 'neutral';

export type ItemRef =
  | { kind: 'opening' }
  | { kind: 'honorary'; id: number }
  | { kind: 'invoice'; id: number }
  | { kind: 'request'; id: number }
  | { kind: 'advance'; id: number }
  | { kind: 'payment'; id: number; invoiceId: number }
  | { kind: 'invoice-recall'; id: number; invoiceId: number }
  | { kind: 'request-recall'; id: number; requestId: number };

export interface TimelineItem {
  key: string;
  ref: ItemRef;
  /** ISO date */
  date: string;
  category: Category | 'opening';
  letter: string;
  title: string;
  sub: string;
  amount: number | null;
  /** Duration of a session, 0 for the other events. */
  minutes: number;
  status: string;
  tone: Tone;
  effects: { text: string; tone: EffectTone }[];
}

export interface MonthGroup {
  key: string;
  label: string;
  summary: string;
  items: TimelineItem[];
}

const MONTHS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

/** Order of the events of a same day, the most recent first: payments after the documents they pay. */
const SAME_DAY_ORDER: Record<ItemRef['kind'], number> = {
  payment: 0, advance: 0, 'invoice-recall': 1, 'request-recall': 1, invoice: 2, request: 2, honorary: 3, opening: 4,
};

export const invoiceRef = (id: number | undefined) => 'F-' + id;
export const requestRef = (id: number | undefined) => 'P-' + id;

export function customerName(customer: Customer | undefined): string {
  if (!customer) {
    return 'client inconnu';
  }
  return customer.fullName || [customer.firstName, customer.lastName].filter(Boolean).join(' ') || 'client ' + customer.id;
}

export function meansText(means: { cash?: boolean; ticketId?: number; iban?: string }): string {
  if (means.cash) {
    return 'Comptant';
  }
  if (means.ticketId) {
    return 'Bulletin QR';
  }
  return 'Virement';
}

/** All the events of the folder, the most recent first. */
export function buildTimeline(data: FolderData, today: string): TimelineItem[] {
  const names = new Map(data.customers.map((c) => [c.id, c]));
  const firstName = (id?: number) => names.get(id ?? -1)?.firstName || customerName(names.get(id ?? -1));
  const items: TimelineItem[] = [];

  items.push({
    key: 'opening',
    ref: { kind: 'opening' },
    date: data.folder.openingDate ?? '',
    category: 'opening',
    letter: 'O',
    title: 'Ouverture du dossier',
    sub: [
      data.folder.type === 'TUTELAGE' ? 'Tutelle' : 'Facturation',
      data.folder.handledBy ? 'géré par ' + data.folder.handledBy : '',
      data.customers.length + (data.customers.length > 1 ? ' clients' : ' client'),
    ].filter(Boolean).join(' · '),
    amount: null,
    minutes: 0,
    status: 'ouvert',
    tone: 'neutral',
    effects: [],
  });

  for (const h of data.honoraries) {
    const invoiced = h.invoiceId != null;
    const expenses = (h.expenses ?? []).reduce((sum, e) => sum + toCents(e.amount), 0);
    items.push({
      key: 'h' + h.id,
      ref: { kind: 'honorary', id: h.id },
      date: h.date ?? '',
      category: 'session',
      letter: 'S',
      title: h.description || 'Séance',
      sub: [
        h.minutes ? `${h.minutes} min · ${formatChf(h.hourlyRate)}/h` : '',
        expenses ? 'frais ' + formatChf(fromCents(expenses)) : '',
      ].filter(Boolean).join(' · '),
      amount: h.amount ?? 0,
      minutes: h.minutes ?? 0,
      status: invoiced ? 'facturée' : 'à facturer',
      tone: invoiced ? 'neutral' : 'open',
      effects: invoiced
        ? [{ text: 'sur ' + invoiceRef(h.invoiceId), tone: 'neutral' }]
        : [{ text: '+' + formatChf(h.amount) + ' à facturer', tone: 'up' }],
    });
  }

  for (const inv of data.invoices) {
    const ref = invoiceRef(inv.id);
    const state = documentState(inv, today);
    items.push({
      key: 'f' + inv.id,
      ref: { kind: 'invoice', id: inv.id },
      date: inv.date ?? '',
      category: 'billing',
      letter: 'F',
      title: 'Facture ' + ref,
      sub: [sharesText(inv.shares, firstName), inv.dueDate ? 'échéance ' + formatDate(inv.dueDate) : ''].filter(Boolean).join(' · '),
      amount: inv.amount ?? 0,
      minutes: 0,
      status: state.status,
      tone: state.tone,
      effects: [
        { text: '−' + formatChf(inv.honorariesSum) + ' à facturer', tone: 'down' as EffectTone },
        ...(toCents(inv.advancedSum) > 0 ? [{ text: '−' + formatChf(inv.advancedSum) + ' provisions', tone: 'neutral' as EffectTone }] : []),
        { text: '+' + formatChf(inv.amount) + ' dû', tone: 'up' as EffectTone },
      ],
    });
    for (const p of inv.payments ?? []) {
      items.push(paymentItem(p, inv, names));
    }
    for (const r of inv.recalls ?? []) {
      items.push({
        key: 'fr' + r.id,
        ref: { kind: 'invoice-recall', id: r.id ?? 0, invoiceId: inv.id },
        date: r.date ?? '',
        category: 'billing',
        letter: 'R',
        title: `Rappel n° ${r.degree} · ${ref}`,
        sub: 'réclame ' + formatChf(r.amount),
        amount: r.amount ?? 0,
        minutes: 0,
        status: 'envoyé',
        tone: 'late',
        effects: [],
      });
    }
  }

  for (const req of data.requests) {
    const ref = requestRef(req.id);
    const state = documentState(req, today);
    const who = (req.shares ?? []).filter((s) => toCents(s.amount) > 0).map((s) => firstName(s.customerId));
    items.push({
      key: 'p' + req.id,
      ref: { kind: 'request', id: req.id },
      date: req.date ?? '',
      category: 'billing',
      letter: 'P',
      title: 'Demande de provision ' + ref,
      sub: [sharesText(req.shares, firstName), req.dueDate ? 'échéance ' + formatDate(req.dueDate) : ''].filter(Boolean).join(' · '),
      amount: req.amount ?? 0,
      minutes: 0,
      status: state.status,
      tone: state.tone,
      effects: [{ text: formatChf(req.amount) + ' demandés' + (who.length ? ' à ' + joinNames(who) : ''), tone: 'neutral' }],
    });
    for (const r of req.recalls ?? []) {
      items.push({
        key: 'pr' + r.id,
        ref: { kind: 'request-recall', id: r.id ?? 0, requestId: req.id },
        date: r.date ?? '',
        category: 'billing',
        letter: 'R',
        title: `Rappel n° ${r.degree} · ${ref}`,
        sub: 'réclame ' + formatChf(r.amount),
        amount: r.amount ?? 0,
        minutes: 0,
        status: 'envoyé',
        tone: 'late',
        effects: [],
      });
    }
  }

  for (const a of data.advances) {
    const deducted = a.invoiceId != null;
    items.push({
      key: 'a' + a.id,
      ref: { kind: 'advance', id: a.id },
      date: a.date ?? '',
      category: 'cash',
      letter: 'A',
      title: 'Avance reçue · ' + customerName(names.get(a.customerId ?? -1)),
      sub: meansText(a) + (a.requestId ? ' · répond à ' + requestRef(a.requestId) : ' · spontanée'),
      amount: a.amount ?? 0,
      minutes: 0,
      status: deducted ? 'déduite' : 'disponible',
      tone: deducted ? 'neutral' : 'done',
      effects: deducted
        ? [{ text: 'déduite sur ' + invoiceRef(a.invoiceId), tone: 'neutral' }]
        : [{ text: '+' + formatChf(a.amount) + ' provision disponible', tone: 'down' }],
    });
  }

  return items.sort((x, y) =>
    y.date.localeCompare(x.date) || SAME_DAY_ORDER[x.ref.kind] - SAME_DAY_ORDER[y.ref.kind] || y.key.localeCompare(x.key),
  );
}

function paymentItem(p: InvoicePayment, inv: Invoice, names: Map<number, Customer>): TimelineItem {
  return {
    key: 'fp' + p.id,
    ref: { kind: 'payment', id: p.id ?? 0, invoiceId: inv.id },
    date: p.date ?? '',
    category: 'cash',
    letter: '€',
    title: 'Paiement reçu · ' + customerName(names.get(p.customerId ?? -1)),
    sub: meansText(p) + ' · ' + invoiceRef(inv.id),
    amount: p.amount ?? 0,
    minutes: 0,
    status: 'reçu',
    tone: 'done',
    effects: [{ text: '−' + formatChf(p.amount) + ' sur ' + invoiceRef(inv.id), tone: 'down' }],
  };
}

interface Payable {
  paid?: boolean;
  paidOn?: string;
  amount?: number;
  remainingSum?: number;
  dueDate?: string;
}

/** Status of an invoice or a request: paid, partly paid, overdue or open. */
export function documentState(doc: Payable, today: string): { status: string; tone: Tone } {
  if (doc.paid) {
    return { status: doc.paidOn ? 'payée le ' + formatDate(doc.paidOn, false) : 'payée', tone: 'done' };
  }
  const overdue = !!doc.dueDate && doc.dueDate < today;
  const remaining = toCents(doc.remainingSum ?? doc.amount);
  if (remaining < toCents(doc.amount)) {
    return { status: 'partiellement payée', tone: overdue ? 'late' : 'open' };
  }
  return overdue ? { status: 'échue', tone: 'late' } : { status: 'ouverte', tone: 'open' };
}

function sharesText(shares: CustomerShare[] | undefined, firstName: (id?: number) => string): string {
  const parts = (shares ?? []).filter((s) => toCents(s.amount) > 0);
  if (parts.length < 2) {
    return '';
  }
  return parts.map((s) => firstName(s.customerId) + ' ' + formatChf(s.amount)).join(' · ');
}

function joinNames(names: string[]): string {
  return names.length <= 1 ? names.join('') : names.slice(0, -1).join(', ') + ' et ' + names[names.length - 1];
}

/** Items grouped by month, the most recent month first, with what was billed and cashed in it. */
export function groupByMonth(items: TimelineItem[]): MonthGroup[] {
  const groups = new Map<string, TimelineItem[]>();
  for (const item of items) {
    const key = item.date.slice(0, 7);
    groups.set(key, [...(groups.get(key) ?? []), item]);
  }
  return [...groups.entries()].map(([key, list]) => {
    const sessions = list.filter((i) => i.ref.kind === 'honorary');
    const minutes = sessions.reduce((sum, i) => sum + i.minutes, 0);
    const billed = sessions.reduce((sum, i) => sum + toCents(i.amount), 0);
    const cashed = list.filter((i) => i.ref.kind === 'payment' || i.ref.kind === 'advance').reduce((sum, i) => sum + toCents(i.amount), 0);
    const [year, month] = key.split('-');
    const parts = [];
    if (sessions.length) {
      parts.push(`${sessions.length} séance${sessions.length > 1 ? 's' : ''} · ${formatMinutes(minutes)} · ${formatChf(fromCents(billed))} d’honoraires`);
    }
    if (cashed) {
      parts.push('encaissé ' + formatChf(fromCents(cashed)));
    }
    return { key, label: `${MONTHS[Number(month) - 1] ?? ''} ${year}`, summary: parts.join(' · '), items: list };
  });
}

export interface CustomerDue {
  customer: Customer;
  /** Still owed on open invoices and requests, from the customer's shares. */
  due: number;
}

export interface FolderFigures {
  toInvoice: number;
  openSessions: number;
  available: number;
  /** Open honoraries not covered by the available advances. */
  uncovered: number;
  dues: CustomerDue[];
  dueTotal: number;
}

export function folderFigures(data: FolderData): FolderFigures {
  const open = data.honoraries.filter((h) => h.invoiceId == null);
  const toInvoice = open.reduce((sum, h) => sum + toCents(h.amount), 0);
  const available = data.advances.filter((a) => a.invoiceId == null).reduce((sum, a) => sum + toCents(a.amount), 0);
  const dues = data.customers.map((customer) => {
    const owed = [...data.invoices, ...data.requests]
      .filter((doc) => !doc.paid)
      .flatMap((doc) => doc.shares ?? [])
      .filter((share) => share.customerId === customer.id)
      .reduce((sum, share) => sum + Math.max(0, toCents(share.remaining)), 0);
    return { customer, due: fromCents(owed) };
  });
  return {
    toInvoice: fromCents(toInvoice),
    openSessions: open.length,
    available: fromCents(available),
    uncovered: fromCents(Math.max(0, toInvoice - available)),
    dues,
    dueTotal: fromCents(dues.reduce((sum, d) => sum + toCents(d.due), 0)),
  };
}
