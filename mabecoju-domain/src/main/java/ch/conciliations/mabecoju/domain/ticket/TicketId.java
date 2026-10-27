package ch.conciliations.mabecoju.domain.ticket;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record TicketId(long value) implements EntityId {

    public static TicketId of(long value) {
        return new TicketId(value);
    }
}
