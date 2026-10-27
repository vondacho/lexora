package io.lexora.domain.ticket;

import io.lexora.domain.shared.EntityId;

public record TicketId(long value) implements EntityId {

    public static TicketId of(long value) {
        return new TicketId(value);
    }
}
