package io.lexora.application.user;

import io.lexora.domain.user.Grant;
import io.lexora.domain.user.User;

import java.util.Set;

/** A user and his grants. */
public record UserAccount(User user, Set<Grant> grants) {
}
