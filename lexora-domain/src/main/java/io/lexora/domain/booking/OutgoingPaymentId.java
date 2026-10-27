package io.lexora.domain.booking;

import io.lexora.domain.shared.EntityId;

public record OutgoingPaymentId(long value) implements EntityId {

    public static OutgoingPaymentId of(long value) {
        return new OutgoingPaymentId(value);
    }
}
