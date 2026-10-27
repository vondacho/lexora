package ch.conciliations.mabecoju.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(Username username);

    List<User> findAll();

    User save(User user);

    void delete(Username username);
}
