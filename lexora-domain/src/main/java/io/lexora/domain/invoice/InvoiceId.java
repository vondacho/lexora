package io.lexora.domain.invoice;

import io.lexora.domain.shared.EntityId;

public record InvoiceId(long value) implements EntityId {

    public static InvoiceId of(long value) {
        return new InvoiceId(value);
    }
}
