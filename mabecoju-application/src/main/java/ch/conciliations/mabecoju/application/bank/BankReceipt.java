package ch.conciliations.mabecoju.application.bank;

import ch.conciliations.mabecoju.domain.billing.PaymentReference.DocumentKind;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

/**
 * A bank credit confirmed for a document.
 *
 * @param customerId the payer; null to let the application choose among the customers who owe something
 * @param reference  payment reference of the credit, to link the payment slip when it is known
 * @param entryReference reference of the bank for the transaction, to import it only once
 */
public record BankReceipt(DocumentKind kind, long documentId, Long customerId, LocalDate date, Money amount,
                          String iban, String reference, String entryReference) {
}
