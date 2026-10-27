package ch.conciliations.mabecoju.application.bank;

import ch.conciliations.mabecoju.domain.shared.Money;

/**
 * @param recorded false when the same payment was already in the books
 * @param settled  the document is now paid
 * @param advance  part of the credit beyond what was owed, booked as a spontaneous advance of the folder
 */
public record BankReceiptResult(boolean recorded, boolean settled, Money advance) {
}
