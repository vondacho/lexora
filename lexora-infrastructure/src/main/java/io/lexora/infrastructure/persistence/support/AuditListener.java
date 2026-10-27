package io.lexora.infrastructure.persistence.support;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;

/** Fills the legacy mute columns with today's date and the authenticated user on every write. */
public class AuditListener {

    /** Width of the legacy {@code *_mute_user} columns. */
    static final int MUTE_USER_LENGTH = 10;

    @PrePersist
    @PreUpdate
    public void stamp(Object entity) {
        if (entity instanceof Audited audited) {
            audited.stamp(LocalDate.now(), currentUsername());
        }
    }

    static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication == null ? "system" : authentication.getName();
        return name.length() > MUTE_USER_LENGTH ? name.substring(0, MUTE_USER_LENGTH) : name;
    }
}
