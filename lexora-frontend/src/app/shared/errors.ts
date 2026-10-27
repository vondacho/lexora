import { Problem } from '../api/generated';
import { lang } from '../i18n/i18n';

type Params = Record<string, unknown>;
type Translate = (p: Params) => string;

/** ISO dates of the parameters written the Swiss way. */
function day(value: unknown): string {
  const text = String(value ?? '');
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text.split('-').reverse().join('.') : text;
}

const v = (p: Params, key: string) => String(p[key] ?? '');

/** Names the API gives to fields and things, in French. */
const NAMES_FR: Record<string, string> = {
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
const nameIn = (names: Record<string, string>) => (p: Params, key: string, fallback: string) => names[v(p, key)] ?? fallback;
const name = nameIn(NAMES_FR);
const capital = (text: string) => text.charAt(0).toUpperCase() + text.slice(1);

const DOCUMENT_TYPES_FR: Record<string, string> = {
  INVOICE: 'les factures',
  ADVANCE_REQUEST: 'les demandes de provision',
  RECALL: 'les rappels',
  UNDEFINED: 'tous les documents',
};

/**
 * Text of each error code of the API (Problem.code), per language. The type makes the lists complete: a code added
 * to the contract does not compile until it is translated in both.
 */
export const ERROR_MESSAGES_FR: Record<Problem.CodeEnum, Translate> = {
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
  MODEL_TYPE_MISMATCH: (p) => `Le modèle « ${v(p, 'model')} » est prévu pour ${DOCUMENT_TYPES_FR[v(p, 'modelType')] ?? 'un autre type de document'}.`,
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

const NAMES_EN: Record<string, string> = {
  'Customer name': 'the name of the client',
  'First name': 'the first name',
  'Last name': 'the last name',
  'Folder name': 'the name of the folder',
  'Folder handler': 'the person in charge',
  'Model name': 'the name of the template',
  'Status name': 'the name of the status',
  'Ticket number': 'the number of the payment slip',
  'User name': 'the user name',
  Password: 'the password',
  Shortcut: 'the initials',
  Wording: 'the wording',
  'Grant module': 'the module of the grant',
  'Grant page': 'the page of the grant',
  amount: 'the amount',
  rate: 'the rate',
  delay: 'the payment term',
  minutes: 'the duration',
  advance: 'the amount of the advance',
  payment: 'the amount of the payment',
  'requested amount': 'the requested amount',
  Folder: 'the folder',
  Customer: 'the client',
  Invoice: 'the invoice',
  'Advance request': 'the advance request',
  'Open invoice': 'the open invoice',
  'Open advance request': 'the open advance request',
  Advance: 'the advance',
  Honorary: 'the session',
  Payment: 'the payment',
  Ticket: 'the payment slip',
  'VAT payment': 'the VAT payment',
  'Standing order': 'the standing order',
  'Document model': 'the letter template',
  User: 'the user',
  Recall: 'the reminder',
};
const nameEn = nameIn(NAMES_EN);

const DOCUMENT_TYPES_EN: Record<string, string> = {
  INVOICE: 'invoices',
  ADVANCE_REQUEST: 'advance requests',
  RECALL: 'reminders',
  UNDEFINED: 'all documents',
};

export const ERROR_MESSAGES_EN: Record<Problem.CodeEnum, Translate> = {
  INVALID_REQUEST: () => 'The request is incomplete or malformed.',
  NOT_FOUND: (p) => capital(nameEn(p, 'entity', 'the item')) + ' does not exist (any more).',
  FORBIDDEN: () => 'Your rights do not allow this operation.',
  FIELD_MANDATORY: (p) => 'Enter ' + nameEn(p, 'field', 'the mandatory field') + '.',
  FIELD_TOO_LONG: (p) => `Text too long for ${nameEn(p, 'field', 'this field')} (${v(p, 'max')} characters at most).`,
  NEGATIVE_VALUE: (p) => `Enter a positive value for ${nameEn(p, 'field', 'this field')}.`,
  AMOUNT_NOT_POSITIVE: (p) => capital(nameEn(p, 'field', 'the amount')) + ' must be above zero.',
  PERIOD_INVERTED: (p) => `The end (${day(p['to'])}) is before the start (${day(p['from'])}).`,
  TOO_MANY_EXPENSES: (p) => `A session has at most ${v(p, 'max')} expenses.`,
  TOO_MANY_LAYOUTS: (p) => `A template has at most ${v(p, 'max')} pages.`,
  DAY_OF_MONTH: () => 'The day of the month is between 1 and 31.',
  FOLDER_TYPE_MANDATORY: () => 'Enter the type of the folder.',
  FOLDER_STATUS_EXISTS: (p) => `The status “${v(p, 'name')}” already exists.`,
  NOT_A_PARTY: () => 'This client is not a party of this folder.',
  OTHER_FOLDER: (p) => `${v(p, 'document') || 'This document'} belongs to another folder.`,
  CUSTOMER_HAS_HISTORY: () =>
    'This client has invoices, requests or payments in the folder: they cannot be deleted. End their mandate instead (“End mandate today”).',
  NO_ACTIVE_CUSTOMER: (p) => `No client of the folder is active on ${day(p['date'])}: check the mandate dates.`,
  HONORARY_INVOICED: (p) => `This session is invoiced on F-${v(p, 'invoiceId')}: delete the invoice first to change it.`,
  NOTHING_TO_INVOICE: () => 'Nothing to invoice: no open session nor available advance.',
  DOCUMENT_PAID: (p) => `${v(p, 'document') || 'This document'} is paid: it is no longer recalled.`,
  RECALL_NOT_OF_DOCUMENT: () => 'This reminder belongs to another document.',
  PAYMENT_NOT_OF_INVOICE: () => 'This payment belongs to another invoice.',
  ADVANCE_DEDUCTED: (p) => `This advance is deducted on F-${v(p, 'invoiceId')}: delete or correct the invoice first.`,
  TICKET_USED: () => 'This payment slip is used by a payment or an advance: it cannot be deleted.',
  PAYMENT_REFERENCE_INVALID: () => 'The payment reference is not valid.',
  NO_CREDITOR_ACCOUNT: () => 'No creditor account is configured: no QR-bill possible.',
  MODEL_NAME_EXISTS: (p) => `A template “${v(p, 'name')}” already exists: choose another name.`,
  MODEL_TYPE_MISMATCH: (p) => `The template “${v(p, 'model')}” is meant for ${DOCUMENT_TYPES_EN[v(p, 'modelType')] ?? 'another kind of document'}.`,
  QR_BILL_INVALID: () => 'The QR-bill cannot be produced: check the address of the client and the account of the office.',
  PAYMENT_FROM_STANDING_ORDER: () => 'This payment comes from a standing order: change the order or delete the payment.',
  PERMANENT_KIND_RESERVED: () => 'Only the execution of a standing order creates a payment of this kind.',
  VAT_PERIOD_OVERLAP: (p) =>
    `The VAT from ${day(p['from'])} to ${day(p['to'])} is already paid (on ${day(p['paidOn'])}): periods cannot overlap.`,
  VAT_PAID_BEFORE_PERIOD: () => 'The VAT of a period is not paid before the period starts.',
  EXPENSE_FOLLOWS_VAT_PAYMENT: (p) =>
    `This expense is the payment of the VAT from ${day(p['from'])} to ${day(p['to'])}: change it from the VAT return.`,
  VAT_ALREADY_BOOKED: () => 'This VAT payment is already booked in the expenses.',
  BANK_FILE_INVALID: () => 'The file is not a readable camt.054 statement: download the credit notification in ISO 20022 format.',
  BANK_RECEIPT_INVALID: () => 'A receipt needs a date and an amount above zero.',
  USER_EXISTS: (p) => `The user “${v(p, 'username')}” already exists.`,
  UNKNOWN_GRANT: (p) => `The grant “${v(p, 'grant')}” does not exist.`,
};

/** The text of a problem returned by the API in the current language, or null when it carries no known code. */
export function translateProblem(problem: Problem | null | undefined): string | null {
  const code = problem?.code;
  const messages = lang() === 'en' ? ERROR_MESSAGES_EN : ERROR_MESSAGES_FR;
  if (!code || !(code in messages)) {
    return null;
  }
  return messages[code](problem?.params ?? {});
}
