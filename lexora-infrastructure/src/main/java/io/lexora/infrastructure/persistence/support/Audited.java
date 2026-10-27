package io.lexora.infrastructure.persistence.support;

import java.time.LocalDate;

/**
 * Entity carrying the legacy "mute" columns ({@code *_mute_date}, {@code *_mute_user}): date and author of the
 * last change.
 */
public interface Audited {

    void stamp(LocalDate date, String username);
}
