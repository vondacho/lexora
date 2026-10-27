package io.lexora.domain.user;

import java.util.Set;

public interface GrantRepository {

    Set<Grant> findByUser(Username username);

    void add(Username username, Grant grant);

    void remove(Username username, Grant grant);

    void removeAll(Username username);
}
