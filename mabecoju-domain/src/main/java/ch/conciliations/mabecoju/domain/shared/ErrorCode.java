package ch.conciliations.mabecoju.domain.shared;

/**
 * Stable codes of the errors returned by the API (Problem Detail {@code code}), translated by the clients; the
 * English {@code detail} is for logs and developers. Parameters, in {@code params}, are named in each description.
 * Never rename a code: add a new one.
 */
public enum ErrorCode {

    // ----- request and access (not domain rules) -----
    /** The request does not follow the contract (missing field, wrong format). */
    INVALID_REQUEST,
    /** {entity, id}: the resource does not exist. */
    NOT_FOUND,
    /** The user lacks the grant of the operation. */
    FORBIDDEN,

    // ----- values -----
    /** {field}: a mandatory text is empty. */
    FIELD_MANDATORY,
    /** {field, max}: a text is too long. */
    FIELD_TOO_LONG,
    /** {field}: an amount, rate, delay or duration below zero. */
    NEGATIVE_VALUE,
    /** {field}: an amount that must be above zero. */
    AMOUNT_NOT_POSITIVE,
    /** {from, to}: a period ending before it starts. */
    PERIOD_INVERTED,
    /** {max}: an honorary holds at most so many expenses. */
    TOO_MANY_EXPENSES,
    /** {max}: a template has at most so many page layouts. */
    TOO_MANY_LAYOUTS,
    /** The execution day of a standing order is between 1 and 31. */
    DAY_OF_MONTH,
    /** A folder needs a type. */
    FOLDER_TYPE_MANDATORY,

    // ----- folders and customers -----
    /** {name}: a folder status of that name exists. */
    FOLDER_STATUS_EXISTS,
    /** {customerId, folderId}: the customer is not a party of the folder. */
    NOT_A_PARTY,
    /** {document}: the document belongs to another folder. */
    OTHER_FOLDER,
    /** {customerId}: the customer has invoices, requests or payments: end the mandate instead of deleting. */
    CUSTOMER_HAS_HISTORY,
    /** {folderId, date}: no customer of the folder is active at the date of the document. */
    NO_ACTIVE_CUSTOMER,

    // ----- billing -----
    /** {honoraryId, invoiceId}: an invoiced honorary can no longer change. */
    HONORARY_INVOICED,
    /** {folderId}: no open honorary nor available advance to invoice. */
    NOTHING_TO_INVOICE,
    /** {document}: a paid invoice or request is not recalled. */
    DOCUMENT_PAID,
    /** {recallId, document}: the reminder belongs to another document. */
    RECALL_NOT_OF_DOCUMENT,
    /** {paymentId, invoiceId}: the payment belongs to another invoice. */
    PAYMENT_NOT_OF_INVOICE,
    /** {advanceId, invoiceId}: an advance deducted on an invoice is not cancelled. */
    ADVANCE_DEDUCTED,
    /** {ticketId}: a payment slip used by a payment or an advance is not deleted. */
    TICKET_USED,
    /** A payment reference cannot be made (identifier too large, invalid reference). */
    PAYMENT_REFERENCE_INVALID,
    /** No QR-bill without a creditor account. */
    NO_CREDITOR_ACCOUNT,

    // ----- documents and templates -----
    /** {name}: a template of that name exists. */
    MODEL_NAME_EXISTS,
    /** {model, modelType, documentType}: the template is meant for another kind of document. */
    MODEL_TYPE_MISMATCH,
    /** {reason}: the QR-bill data is not valid. */
    QR_BILL_INVALID,

    // ----- books -----
    /** {paymentId}: a payment booked by a standing order is not modified. */
    PAYMENT_FROM_STANDING_ORDER,
    /** Only a standing order execution books a permanent payment. */
    PERMANENT_KIND_RESERVED,
    /** {from, to, paidOn}: the period overlaps a VAT payment. */
    VAT_PERIOD_OVERLAP,
    /** The VAT of a period is not paid before the period starts. */
    VAT_PAID_BEFORE_PERIOD,
    /** {paymentId, from, to}: the expense follows its VAT payment. */
    EXPENSE_FOLLOWS_VAT_PAYMENT,
    /** The VAT payment is already booked as an expense. */
    VAT_ALREADY_BOOKED,

    // ----- bank import -----
    /** The file is not a readable camt.054 notification. */
    BANK_FILE_INVALID,
    /** A bank receipt needs a date and a positive amount. */
    BANK_RECEIPT_INVALID,

    // ----- users -----
    /** {username}: a user of that name exists. */
    USER_EXISTS,
    /** {grant}: the grant is not known. */
    UNKNOWN_GRANT
}
