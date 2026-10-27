package io.lexora.domain.customer;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;

import java.time.LocalDate;

/**
 * Period during which a customer is a party of the folder and receives its documents
 * (legacy columns {@code cust_ibs} / {@code cust_abs}).
 *
 * @param since null when the customer has always been a party
 * @param until null while still active
 */
public record ActivityPeriod(LocalDate since, LocalDate until) {

    public static final ActivityPeriod ALWAYS = new ActivityPeriod(null, null);

    public ActivityPeriod {
        if (since != null && until != null && until.isBefore(since)) {
            throw new DomainException(ErrorCode.PERIOD_INVERTED, "Activity end " + until + " is before its start " + since)
                    .with("from", since)
                    .with("to", until);
        }
    }

    /** Same rule as the legacy {@code Folder::findActive}: an unknown start means always active. */
    public boolean includes(LocalDate date) {
        if (since == null) {
            return true;
        }
        return !since.isAfter(date) && (until == null || !until.isBefore(date));
    }
}
