package ch.conciliations.mabecoju.domain.billing;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

/**
 * A payment reminder ("rappel") sent for an invoice or an advance request.
 *
 * @param degree 1 for the first reminder, 2 for the second, ...
 * @param amount amount still due when the reminder was issued
 */
public record Recall(RecallId id, LocalDate date, int degree, Money amount) {
}
