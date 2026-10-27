package ch.conciliations.mabecoju.domain.billing;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record RecallId(long value) implements EntityId {

    public static RecallId of(long value) {
        return new RecallId(value);
    }
}
