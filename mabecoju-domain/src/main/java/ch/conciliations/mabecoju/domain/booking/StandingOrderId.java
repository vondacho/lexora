package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record StandingOrderId(long value) implements EntityId {

    public static StandingOrderId of(long value) {
        return new StandingOrderId(value);
    }
}
