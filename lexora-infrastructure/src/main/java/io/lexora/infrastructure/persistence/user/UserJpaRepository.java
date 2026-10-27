package io.lexora.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findAllByOrderByUsernameAsc();
}
