package ch.conciliations.mabecoju.infrastructure.persistence.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderStatusJpaRepository extends JpaRepository<FolderStatusEntity, Integer> {

    List<FolderStatusEntity> findAllByOrderByNameAsc();

    Optional<FolderStatusEntity> findFirstByNameIgnoreCase(String name);
}
