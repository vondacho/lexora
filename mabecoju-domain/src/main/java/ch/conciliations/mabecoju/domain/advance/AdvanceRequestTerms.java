package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @param meetingDate date of the meeting the provision is requested for, if any
 * @param delayDays   payment term in days
 * @param amount      requested amount
 */
public record AdvanceRequestTerms(
        LocalDate date,
        LocalDate meetingDate,
        int delayDays,
        Money amount,
        Distribution distribution,
        String comment) {

    public AdvanceRequestTerms {
        Objects.requireNonNull(date, "date");
        if (amount == null || !amount.isPositive()) {
            throw new DomainException(ErrorCode.AMOUNT_NOT_POSITIVE, "A requested amount must be positive")
                    .with("field", "requested amount");
        }
        if (delayDays < 0) {
            throw new DomainException(ErrorCode.NEGATIVE_VALUE, "Payment delay cannot be negative")
                    .with("field", "delay");
        }
        distribution = distribution == null ? Distribution.NONE : distribution;
    }
}
