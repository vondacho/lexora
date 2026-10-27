package io.lexora.domain.user;

import io.lexora.domain.shared.Strings;

public record Username(String value) {

    public Username {
        value = Strings.requireNotBlank(value, "User name");
        Strings.requireMaxLength(value, 16, "User name");
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
