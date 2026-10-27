package io.lexora.domain.billing;

import io.lexora.domain.shared.EntityId;

public record RecallId(long value) implements EntityId {

    public static RecallId of(long value) {
        return new RecallId(value);
    }
}
