package ch.conciliations.mabecoju.domain.honorary;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record HonoraryId(long value) implements EntityId {

    public static HonoraryId of(long value) {
        return new HonoraryId(value);
    }
}
