package ch.conciliations.mabecoju.domain.customer;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record CustomerId(long value) implements EntityId {

    public static CustomerId of(long value) {
        return new CustomerId(value);
    }
}
