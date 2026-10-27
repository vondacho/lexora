package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

/**
 * A debt of one customer: his share of an invoice or of an advance request.
 *
 * @param documentId invoice or advance request id
 */
public record OpenItem(long documentId, LocalDate date, LocalDate dueDate, Money share, Money paid, Money remaining) {
}
