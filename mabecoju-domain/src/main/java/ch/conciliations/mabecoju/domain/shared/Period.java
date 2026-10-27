package ch.conciliations.mabecoju.domain.shared;

import java.time.LocalDate;
import java.util.Objects;

/** An inclusive date range used by reports and searches. */
public record Period(LocalDate from, LocalDate to) {

    public Period {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new DomainException(ErrorCode.PERIOD_INVERTED, "Period end " + to + " is before its start " + from)
                    .with("from", from)
                    .with("to", to);
        }
    }

    public static Period of(LocalDate from, LocalDate to) {
        return new Period(from, to);
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }
}
