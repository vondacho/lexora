package ch.conciliations.mabecoju.infrastructure.persistence.user;

import ch.conciliations.mabecoju.domain.user.Grant;
import ch.conciliations.mabecoju.domain.user.GrantRepository;
import ch.conciliations.mabecoju.domain.user.Username;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

@Repository
class GrantRepositoryAdapter implements GrantRepository {

    private final GrantJpaRepository jpa;

    GrantRepositoryAdapter(GrantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Set<Grant> findByUser(Username username) {
        Set<Grant> grants = new LinkedHashSet<>();
        jpa.findByUsername(username.value()).stream()
                .filter(g -> g.module != null && g.page != null)
                .forEach(g -> grants.add(Grant.of(g.module, g.page)));
        return grants;
    }

    @Override
    public void add(Username username, Grant grant) {
        GrantEntity e = new GrantEntity();
        e.username = username.value();
        e.module = grant.module();
        e.page = grant.page();
        jpa.saveAndFlush(e);
    }

    @Override
    public void remove(Username username, Grant grant) {
        jpa.deleteGrant(username.value(), grant.module(), grant.page());
    }

    @Override
    public void removeAll(Username username) {
        jpa.deleteAllOf(username.value());
    }
}
