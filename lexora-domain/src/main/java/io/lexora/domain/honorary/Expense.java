package io.lexora.domain.honorary;

import io.lexora.domain.shared.Money;

import java.util.Objects;

/** Out-of-pocket expense charged together with a service (travel, copies, ...). */
public record Expense(Money amount, String description) {

    public Expense {
        Objects.requireNonNull(amount, "amount");
    }
}
