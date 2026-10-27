package io.lexora.domain.booking;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;

/**
 * @param dayOfMonth day of the month the order is executed (1-31, clamped to the month length)
 */
public record StandingOrderDetails(
        String creditor,
        String title,
        Money amount,
        Rate taxRate,
        int dayOfMonth,
        boolean enabled) {

    public StandingOrderDetails {
        amount = amount == null ? Money.ZERO : amount;
        taxRate = taxRate == null ? Rate.ZERO : taxRate;
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new DomainException(ErrorCode.DAY_OF_MONTH, "Execution day must be between 1 and 31");
        }
    }
}
