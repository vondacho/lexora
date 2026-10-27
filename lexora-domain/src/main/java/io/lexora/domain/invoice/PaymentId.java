package io.lexora.domain.invoice;

import io.lexora.domain.shared.EntityId;

/** Identifier of a payment received for an invoice. */
public record PaymentId(long value) implements EntityId {

    public static PaymentId of(long value) {
        return new PaymentId(value);
    }
}
