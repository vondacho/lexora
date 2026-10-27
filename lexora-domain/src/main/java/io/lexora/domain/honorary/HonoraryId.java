package io.lexora.domain.honorary;

import io.lexora.domain.shared.EntityId;

public record HonoraryId(long value) implements EntityId {

    public static HonoraryId of(long value) {
        return new HonoraryId(value);
    }
}
