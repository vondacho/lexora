package io.lexora.infrastructure.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentModelJpaRepository extends JpaRepository<DocumentModelEntity, Integer> {

    Optional<DocumentModelEntity> findFirstByName(String name);

    List<DocumentModelEntity> findAllByOrderByNameAscTypeAsc();

    List<DocumentModelEntity> findByTypeOrderByNameAsc(Integer type);
}
