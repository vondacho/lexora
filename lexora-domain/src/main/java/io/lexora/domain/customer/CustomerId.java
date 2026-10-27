package io.lexora.domain.customer;

import io.lexora.domain.shared.EntityId;

public record CustomerId(long value) implements EntityId {

    public static CustomerId of(long value) {
        return new CustomerId(value);
    }
}
