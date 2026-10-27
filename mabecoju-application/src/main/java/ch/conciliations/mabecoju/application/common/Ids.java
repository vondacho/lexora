package ch.conciliations.mabecoju.application.common;

import java.util.Optional;

/** Helpers to load aggregates or fail with {@link NotFoundException}. */
public final class Ids {

    private Ids() {
    }

    public static <T> T require(Optional<T> found, String what, Object id) {
        return found.orElseThrow(() -> new NotFoundException(what, id));
    }
}
