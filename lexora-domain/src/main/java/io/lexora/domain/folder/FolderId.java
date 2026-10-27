package io.lexora.domain.folder;

import io.lexora.domain.shared.EntityId;

public record FolderId(long value) implements EntityId {

    public static FolderId of(long value) {
        return new FolderId(value);
    }
}
