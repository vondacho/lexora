package ch.conciliations.mabecoju.domain.template;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record DocumentModelId(long value) implements EntityId {

    public static DocumentModelId of(long value) {
        return new DocumentModelId(value);
    }
}
