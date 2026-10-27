package io.lexora.domain.booking;

import io.lexora.domain.shared.EntityId;

public record VatPaymentId(long value) implements EntityId {

    public static VatPaymentId of(long value) {
        return new VatPaymentId(value);
    }
}
