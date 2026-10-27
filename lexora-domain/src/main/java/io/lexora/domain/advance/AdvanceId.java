package io.lexora.domain.advance;

import io.lexora.domain.shared.EntityId;

public record AdvanceId(long value) implements EntityId {

    public static AdvanceId of(long value) {
        return new AdvanceId(value);
    }
}
