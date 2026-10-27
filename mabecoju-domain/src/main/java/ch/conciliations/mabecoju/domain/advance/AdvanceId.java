package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record AdvanceId(long value) implements EntityId {

    public static AdvanceId of(long value) {
        return new AdvanceId(value);
    }
}
