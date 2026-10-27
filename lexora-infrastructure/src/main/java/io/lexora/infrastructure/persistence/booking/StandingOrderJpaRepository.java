package io.lexora.infrastructure.persistence.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandingOrderJpaRepository extends JpaRepository<StandingOrderEntity, Integer> {

    List<StandingOrderEntity> findByFolderIdIsNullOrderByIdDesc();

    List<StandingOrderEntity> findByFolderIdOrderByIdDesc(Integer folderId);
}
