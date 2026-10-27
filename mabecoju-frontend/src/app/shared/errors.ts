import { Problem } from '../api/generated';

type Params = Record<string, unknown>;
type Translate = (p: Params) => string;

/** ISO dates of the parameters written the Swiss way. */
function day(value: unknown): string {
  const text = String(value ?? '');
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text.split('-').reverse().join('.') : text;
}

const v = (p: Params, key: string) => String(p[key] ?? '');

/** Names the API gives to fields and things, in French. */
const NAMES: Record<string, string> = {
  'Customer name': 'le nom du client',
  'First name': 'le prénom',
  'Last name': 'le nom',
  'Folder name': 'le nom du dossier',
  'Folder handler': 'la personne en charge',
  'Model name': 'le nom du modèle',
  'Status name': 'le nom du statut',
  'Ticket number': 'le numéro du bulletin',
  'User name': 'le nom d’utilisateur',
  Password: 'le mot de passe',
  Shortcut: 'les initiales',
  Wording: 'le libellé',
  'Grant module': 'le module du droit',
  'Grant page': 'la page du droit',
  amount: 'le montant',
  rate: 'le taux',
  delay: 'le délai',
  minutes: 'la durée',
  advance: 'le montant de l’avance',
  payment: 'le montant du paiement',
  'requested amount': 'le montant demandé',
  Folder: 'le dossier',
  Customer: 'le client',
  Invoice: 'la facture',
  'Advance request': 'la demande de provision',
  'Open invoice': 'la facture ouverte',
  'Open advance request': 'la demande de provision ouverte',
  Advance: 'l’avance',
  Honorary: 'la séance',
  Payment: 'le paiement',
  Ticket: 'le bulletin',
  'VAT payment': 'le paiement de TVA',
  'Standing order': 'l’ordre permanent',
  'Document model': 'le modèle de lettre',
  User: 'l’utilisateur',
  Recall: 'le rappel',
};
const name = (p: Params, key: string, fallback: string) => NAMES[v(p, key)] ?? fallback;
const capital = (text: string) => text.charAt(0).toUpperCase() + text.slice(1);

const DOCUMENT_TYPES: Record<string, string> = {
  INVOICE: 'les factures',
  ADVANCE_REQUEST: 'les demandes de provision',
  RECALL: 'les rappels',
  UNDEFINED: 'tous les documents',
};

/**
 * French text of each error code of the API (Problem.code). The type makes the list complete: a code added to the
 * contract does not compile until it is translated here.
 */
export const ERROR_MESSAGES: Record<Problem.CodeEnum, Translate> = {
  INVALID_REQUEST: () => 'La demande est incomplète ou mal formée.',
  NOT_FOUND: (p) => capital(name(p, 'entity', 'l’élément')) + ' n’existe pas ou plus.',
  FORBIDDEN: () => 'Vos droits ne permettent pas cette opération.',
  FIELD_MANDATORY: (p) => 'Indiquez ' + name(p, 'field', 'le champ obligatoire') + '.',
  FIELD_TOO_LONG: (p) => `Texte trop long pour ${name(p, 'field', 'ce champ')} (${v(p, 'max')} caractères au plus).`,
  NEGATIVE_VALUE: (p) => `Indiquez une valeur positive pour ${name(p, 'field', 'ce champ')}.`,
  AMOUNT_NOT_POSITIVE: (p) => capital(name(p, 'field', 'le montant')) + ' doit être supérieur à zéro.',
  PERIOD_INVERTED: (p) => `La fin (${day(p['to'])}) précède le début (${day(p['from'])}).`,
  TOO_MANY_EXPENSES: (p) => `Une séance a au plus ${v(p, 'max')} frais.`,
  TOO_MANY_LAYOUTS: (p) => `Un modèle a au plus ${v(p, 'max')} pages.`,
  DAY_OF_MONTH: () => 'Le jour du mois est compris entre 1 et 31.',
  FOLDER_TYPE_MANDATORY: () => 'Indiquez le type du dossier.',
  FOLDER_STATUS_EXISTS: (p) => `Le statut « ${v(p, 'name')} » existe déjà.`,
  NOT_A_PARTY: () => 'Ce client n’est pas partie à ce dossier.',
  OTHER_FOLDER: (p) => `${v(p, 'document') || 'Ce document'} appartient à un autre dossier.`,
  CUSTOMER_HAS_HISTORY: () =>
    'Ce client a des factures, demandes ou paiements dans le dossier : il ne se supprime pas. Terminez plutôt son mandat (« Fin de mandat aujourd’hui »).',
  NO_ACTIVE_CUSTOMER: (p) => `Aucun client du dossier n’est actif au ${day(p['date'])} : vérifiez les dates de mandat.`,
  HONORARY_INVOICED: (p) => `Cette séance est facturée sur F-${v(p, 'invoiceId')} : supprimez d’abord la facture pour la changer.`,
  NOTHING_TO_INVOICE: () => 'Rien à facturer : aucune séance ouverte ni provision disponible.',
  DOCUMENT_PAID: (p) => `${v(p, 'document') || 'Ce document'} est payé : il ne se rappelle plus.`,
  RECALL_NOT_OF_DOCUMENT: () => 'Ce rappel appartient à un autre document.',
  PAYMENT_NOT_OF_INVOICE: () => 'Ce paiement appartient à une autre facture.',
  ADVANCE_DEDUCTED: (p) => `Cette avance est déduite sur F-${v(p, 'invoiceId')} : supprimez ou corrigez d’abord la facture.`,
  TICKET_USED: () => 'Ce bulletin est utilisé par un paiement ou une avance : il ne se supprime pas.',
  PAYMENT_REFERENCE_INVALID: () => 'La référence de paiement n’est pas valable.',
  NO_CREDITOR_ACCOUNT: () => 'Aucun compte créancier n’est configuré : pas de bulletin QR possible.',
  MODEL_NAME_EXISTS: (p) => `Un modèle « ${v(p, 'name')} » existe déjà : choisissez un autre nom.`,
  MODEL_TYPE_MISMATCH: (p) => `Le modèle « ${v(p, 'model')} » est prévu pour ${DOCUMENT_TYPES[v(p, 'modelType')] ?? 'un autre type de document'}.`,
  QR_BILL_INVALID: () => 'Le bulletin QR ne peut pas être produit : vérifiez l’adresse du client et le compte du bureau.',
  PAYMENT_FROM_STANDING_ORDER: () => 'Ce paiement vient d’un ordre permanent : changez l’ordre ou supprimez le paiement.',
  PERMANENT_KIND_RESERVED: () => 'Seule l’exécution d’un ordre permanent crée un paiement de ce type.',
  VAT_PERIOD_OVERLAP: (p) =>
    `La TVA du ${day(p['from'])} au ${day(p['to'])} est déjà payée (le ${day(p['paidOn'])}) : les périodes ne peuvent pas se chevaucher.`,
  VAT_PAID_BEFORE_PERIOD: () => 'La TVA d’une période ne se paie pas avant son début.',
  EXPENSE_FOLLOWS_VAT_PAYMENT: (p) =>
    `Cette dépense est le paiement de la TVA du ${day(p['from'])} au ${day(p['to'])} : modifiez-la depuis le décompte TVA.`,
  VAT_ALREADY_BOOKED: () => 'Ce paiement de TVA est déjà comptabilisé dans les dépenses.',
  BANK_FILE_INVALID: () => 'Le fichier n’est pas un relevé camt.054 lisible : téléchargez la notification de crédit au format ISO 20022.',
  BANK_RECEIPT_INVALID: () => 'Un encaissement demande une date et un montant supérieur à zéro.',
  USER_EXISTS: (p) => `L’utilisateur « ${v(p, 'username')} » existe déjà.`,
  UNKNOWN_GRANT: (p) => `Le droit « ${v(p, 'grant')} » n’existe pas.`,
};

/** The French text of a problem returned by the API, or null when it carries no known code. */
export function translateProblem(problem: Problem | null | undefined): string | null {
  const code = problem?.code;
  if (!code || !(code in ERROR_MESSAGES)) {
    return null;
  }
  return ERROR_MESSAGES[code](problem?.params ?? {});
}
