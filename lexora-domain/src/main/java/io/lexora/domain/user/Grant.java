package io.lexora.domain.user;

import io.lexora.domain.shared.Strings;

/**
 * Permission to use one page (feature) of a module ({@code all_grants}).
 */
public record Grant(String module, String page) {

    public Grant {
        Strings.requireNotBlank(module, "Grant module");
        Strings.requireNotBlank(page, "Grant page");
    }

    public static Grant of(String module, String page) {
        return new Grant(module, page);
    }

    /** Authority name, {@code module:page}. */
    public String authority() {
        return module + ":" + page;
    }
}
