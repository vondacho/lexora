package io.lexora.infrastructure.persistence.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingDefaultsJpaRepository extends JpaRepository<BookingDefaultsEntity, Integer> {

    Optional<BookingDefaultsEntity> findFirstByOrderByIdAsc();
}
