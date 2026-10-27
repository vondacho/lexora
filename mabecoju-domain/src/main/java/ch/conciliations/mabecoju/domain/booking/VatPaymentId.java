package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record VatPaymentId(long value) implements EntityId {

    public static VatPaymentId of(long value) {
        return new VatPaymentId(value);
    }
}
