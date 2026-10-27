package ch.conciliations.mabecoju.application.user;

import ch.conciliations.mabecoju.domain.user.Grant;
import ch.conciliations.mabecoju.domain.user.User;

import java.util.Set;

/** A user and his grants. */
public record UserAccount(User user, Set<Grant> grants) {
}
