package io.lexora.domain.advance;

import io.lexora.domain.shared.EntityId;

public record AdvanceRequestId(long value) implements EntityId {

    public static AdvanceRequestId of(long value) {
        return new AdvanceRequestId(value);
    }
}
