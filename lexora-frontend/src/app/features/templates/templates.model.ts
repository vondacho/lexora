import { DocumentModel, DocumentType, PageLayout } from '../../api/generated';
import { texts } from '../../i18n/i18n';

/** Name of a document type, in the current language. */
export const typeLabel = (type: DocumentType) => texts().templates.types[type];

const GROUP_TYPES: DocumentType[] = ['INVOICE', 'ADVANCE_REQUEST', 'RECALL', 'UNDEFINED'];

export interface Field {
  token: string;
  label: string;
  help: string;
}

/** The placeholders the API fills when printing (same names as the legacy templates). */
const FIELD_TOKENS = [
  '$title_1', '$firstName_1', '$lastName_1', '$amount_1', '$amount_2', '$amount', '$title_2', '$firstName_2',
  '$refNo', '$date', '$delay', '$dueDate', '$invoiceDate', '$lastRecallDate', '$issuerName', '$tvaNo',
];

/** The placeholders offered as chips, with their label and help in the current language. */
export function fields(): Field[] {
  const t = texts().templates.fields;
  return FIELD_TOKENS.map((token) => ({ token, label: t[token][0], help: t[token][1] }));
}

const KNOWN = new Set([
  'issuerName', 'issuerShortcut', 'refNo', 'tvaNo', 'date', 'delay', 'dueDate', 'modelTitle', 'invoiceDate', 'lastRecallDate', 'amount',
  ...[1, 2, 3].flatMap((n) => ['title', 'firstName', 'lastName', 'streetNo', 'zip', 'city', 'amount'].map((f) => `${f}_${n}`)),
]);

const PLACEHOLDER = /\$\{([A-Za-z_][A-Za-z0-9_]*)}|\$([A-Za-z_][A-Za-z0-9_]*)/g;

/** Placeholders the API does not know: they would print nothing. */
export function unknownFields(content: string): string[] {
  const found = [...content.matchAll(PLACEHOLDER)].map((m) => m[1] ?? m[2]);
  return [...new Set(found.filter((name) => !KNOWN.has(name)))];
}

export function mentionsAmount(content: string): boolean {
  return [...content.matchAll(PLACEHOLDER)].some((m) => /^amount(_\d)?$/.test(m[1] ?? m[2]));
}

/** Same rule as the API: templates written before the HTML editor are plain text. */
export function isHtml(text: string): boolean {
  return /<(p|br|div|span|b|strong|i|em|u|ul|ol|li|table|h[1-6])\b/i.test(text);
}

function escape(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/** A plain-text template as HTML: one paragraph per block, line breaks kept. */
export function textToHtml(text: string): string {
  const blocks = text.replace(/\r\n/g, '\n').split(/\n{2,}/).map((b) => b.trim()).filter(Boolean);
  return blocks.map((b) => '<p>' + escape(b).replace(/\n/g, '<br>') + '</p>').join('');
}

export type PageKind = 'letter' | 'detail';

/** Pages of each copy: a layout named "…detail…" is the itemised statement, the others the letter. */
export function pagesOf(layouts: PageLayout[] | undefined): PageKind[] {
  const pages = (layouts ?? []).filter((l) => l.layout?.trim()).map((l): PageKind => (l.layout!.includes('detail') ? 'detail' : 'letter'));
  return pages.length ? pages : ['letter'];
}

/**
 * Layouts for the pages chosen. The names the template already used are kept (the legacy application still reads
 * them); new pages get a name the API recognises.
 */
export function layoutsFor(pages: PageKind[], previous: PageLayout[] | undefined, type: DocumentType): PageLayout[] {
  const used = (previous ?? []).filter((l) => l.layout?.trim());
  const letterName = used.find((l) => !l.layout!.includes('detail'))?.layout ?? defaultLetter(type);
  const detailName = used.find((l) => l.layout!.includes('detail'))?.layout ?? 'layout_invoice_detail.php';
  return pages.map((p, i) => ({
    layout: p === 'detail' ? detailName : letterName,
    singlePage: used[i]?.singlePage ?? false,
  }));
}

function defaultLetter(type: DocumentType): string {
  return type === 'ADVANCE_REQUEST' ? 'layout_request.php' : type === 'RECALL' ? 'layout_recall.php' : 'layout_invoice.php';
}

/** Values of a sample document for the preview: a folder with two clients sharing 864.80. */
export function sampleValues(type: DocumentType, copy: 0 | 1, title: string): Record<string, string> {
  const anne = { title: 'Madame', firstName: 'Anne', lastName: 'Dupont', street: 'Rue des Alpes 12', zip: '2000', city: 'Neuchâtel' };
  const paul = { title: 'Monsieur', firstName: 'Paul', lastName: 'Dupont', street: 'Chemin du Lac 3', zip: '2001', city: 'Neuchâtel' };
  const [first, second] = copy === 0 ? [anne, paul] : [paul, anne];
  const values: Record<string, string> = {
    issuerName: 'Marna Müller-Bessi',
    issuerShortcut: 'mmb',
    refNo: type === 'ADVANCE_REQUEST' ? '374' : '2232',
    tvaNo: '000 000',
    date: '26 octobre 2026',
    delay: type === 'ADVANCE_REQUEST' ? '10' : '30',
    dueDate: '25 novembre 2026',
    modelTitle: title,
    invoiceDate: type === 'RECALL' ? '24 septembre 2026' : '26 octobre 2026',
    lastRecallDate: type === 'RECALL' ? '12.10.2026' : '',
    amount: '432.40',
  };
  [first, second].forEach((c, i) => {
    const n = '_' + (i + 1);
    values['title' + n] = c.title;
    values['firstName' + n] = c.firstName;
    values['lastName' + n] = c.lastName;
    values['streetNo' + n] = c.street;
    values['zip' + n] = c.zip;
    values['city' + n] = c.city;
    values['amount' + n] = '432.40';
  });
  return values;
}

/** Templates of a type, alphabetically, for the list. */
export function grouped(models: DocumentModel[]): { type: DocumentType; label: string; items: DocumentModel[] }[] {
  return GROUP_TYPES.map((type) => ({
    type,
    label: texts().templates.groups[type],
    items: models.filter((m) => (m.type ?? 'UNDEFINED') === type).sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '')),
  })).filter((g) => g.items.length > 0);
}
