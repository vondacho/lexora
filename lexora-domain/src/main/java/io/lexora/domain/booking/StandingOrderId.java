package io.lexora.domain.booking;

import io.lexora.domain.shared.EntityId;

public record StandingOrderId(long value) implements EntityId {

    public static StandingOrderId of(long value) {
        return new StandingOrderId(value);
    }
}
