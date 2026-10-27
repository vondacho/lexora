package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record AdvanceRequestId(long value) implements EntityId {

    public static AdvanceRequestId of(long value) {
        return new AdvanceRequestId(value);
    }
}
