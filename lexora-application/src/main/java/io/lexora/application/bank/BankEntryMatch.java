package io.lexora.application.bank;

import io.lexora.domain.billing.PaymentReference.DocumentKind;
import io.lexora.domain.shared.Money;

/**
 * What a bank credit pays, as far as the books can tell.
 *
 * @param target    the document and customer it pays, or a suggestion; null when unknown
 * @param remaining what the customer still owed on the target before this credit
 */
public record BankEntryMatch(BankCredit credit, Status status, Problem problem, Target target, Money remaining) {

    public enum Status {
        /** Paid by reference, for an amount the document accepts. */
        READY,
        /** A suggestion or an amount above what is owed: to confirm. */
        CHECK,
        /** No document found: to associate by hand or leave aside. */
        UNMATCHED,
        /** Already in the books (same document, customer, date and amount). */
        DUPLICATE
    }

    public enum Problem {
        NONE,
        /** No reference, or one of this application not known: suggested by amount and name. */
        SUGGESTED,
        /** The credit exceeds what is owed: the rest would become an advance. */
        OVERPAID,
        /** The reference points to a document that is already paid. */
        DOCUMENT_PAID,
        /** The reference is not one of this application. */
        UNKNOWN_REFERENCE,
        /** Neither a reference nor a document with that amount. */
        NOT_FOUND,
        /** Not in Swiss francs. */
        CURRENCY
    }

    public record Target(DocumentKind kind, long documentId, long folderId, String folderName, long customerId,
                         String customerName) {
    }
}
