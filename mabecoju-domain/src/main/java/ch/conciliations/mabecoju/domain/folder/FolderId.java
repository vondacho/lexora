package ch.conciliations.mabecoju.domain.folder;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record FolderId(long value) implements EntityId {

    public static FolderId of(long value) {
        return new FolderId(value);
    }
}
