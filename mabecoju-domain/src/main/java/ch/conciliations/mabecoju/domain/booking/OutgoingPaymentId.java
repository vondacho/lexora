package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record OutgoingPaymentId(long value) implements EntityId {

    public static OutgoingPaymentId of(long value) {
        return new OutgoingPaymentId(value);
    }
}
