package io.lexora.infrastructure.persistence.folder;

import io.lexora.domain.folder.FolderStatus;
import io.lexora.domain.folder.FolderStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class FolderStatusRepositoryAdapter implements FolderStatusRepository {

    private final FolderStatusJpaRepository jpa;

    FolderStatusRepositoryAdapter(FolderStatusJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<FolderStatus> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(FolderStatusRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<FolderStatus> findByName(String name) {
        return jpa.findFirstByNameIgnoreCase(name).map(FolderStatusRepositoryAdapter::toDomain);
    }

    @Override
    public FolderStatus add(String name) {
        FolderStatusEntity entity = new FolderStatusEntity();
        entity.name = name;
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public void delete(long id) {
        jpa.deleteById(toInt(id));
    }

    private static FolderStatus toDomain(FolderStatusEntity e) {
        return new FolderStatus(e.id.longValue(), e.name);
    }
}
