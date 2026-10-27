package io.lexora.infrastructure.persistence.user;

import io.lexora.domain.user.User;
import io.lexora.domain.user.UserRepository;
import io.lexora.domain.user.Username;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpa.findById(username.value()).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAllByOrderByUsernameAsc().stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserEntity e = jpa.findById(user.username().value()).orElseGet(UserEntity::new);
        e.username = user.username().value();
        e.password = user.passwordHash();
        e.firstName = user.firstName();
        e.lastName = user.lastName();
        e.shortcut = user.shortcut();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(Username username) {
        jpa.deleteById(username.value());
    }

    static User toDomain(UserEntity e) {
        return User.restore(Username.of(e.username), e.password, e.firstName, e.lastName, e.shortcut);
    }
}
