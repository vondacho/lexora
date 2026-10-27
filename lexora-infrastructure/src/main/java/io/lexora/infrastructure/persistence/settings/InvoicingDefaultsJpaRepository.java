package io.lexora.infrastructure.persistence.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoicingDefaultsJpaRepository extends JpaRepository<InvoicingDefaultsEntity, Integer> {

    Optional<InvoicingDefaultsEntity> findFirstByOrderByIdAsc();
}
