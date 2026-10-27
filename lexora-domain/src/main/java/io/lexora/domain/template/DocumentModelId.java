package io.lexora.domain.template;

import io.lexora.domain.shared.EntityId;

public record DocumentModelId(long value) implements EntityId {

    public static DocumentModelId of(long value) {
        return new DocumentModelId(value);
    }
}
