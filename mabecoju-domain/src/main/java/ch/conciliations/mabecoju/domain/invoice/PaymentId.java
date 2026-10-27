package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.shared.EntityId;

/** Identifier of a payment received for an invoice. */
public record PaymentId(long value) implements EntityId {

    public static PaymentId of(long value) {
        return new PaymentId(value);
    }
}
