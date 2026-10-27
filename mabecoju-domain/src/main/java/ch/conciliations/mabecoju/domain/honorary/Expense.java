package ch.conciliations.mabecoju.domain.honorary;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.util.Objects;

/** Out-of-pocket expense charged together with a service (travel, copies, ...). */
public record Expense(Money amount, String description) {

    public Expense {
        Objects.requireNonNull(amount, "amount");
    }
}
