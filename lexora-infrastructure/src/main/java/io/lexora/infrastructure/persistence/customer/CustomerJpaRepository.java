package io.lexora.infrastructure.persistence.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Integer> {

    List<CustomerEntity> findByFolderIdOrderByActiveSinceAscIdAsc(Integer folderId);
}
