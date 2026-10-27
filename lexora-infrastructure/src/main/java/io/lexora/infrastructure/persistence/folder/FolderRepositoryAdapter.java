package io.lexora.infrastructure.persistence.folder;

import io.lexora.domain.folder.Folder;
import io.lexora.domain.folder.FolderDetails;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.folder.FolderSearch;
import io.lexora.domain.folder.FolderType;
import io.lexora.domain.shared.Strings;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class FolderRepositoryAdapter implements FolderRepository {

    private final FolderJpaRepository jpa;

    FolderRepositoryAdapter(FolderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Folder> findById(FolderId id) {
        return jpa.findById(toInt(id.value())).map(FolderRepositoryAdapter::toDomain);
    }

    @Override
    public Map<FolderId, Folder> findAllById(Collection<FolderId> ids) {
        Map<FolderId, Folder> result = new LinkedHashMap<>();
        jpa.findAllById(ids.stream().map(id -> toInt(id.value())).toList())
                .forEach(e -> result.put(FolderId.of(e.id), toDomain(e)));
        return result;
    }

    @Override
    public List<Folder> search(FolderSearch search) {
        Integer type = search.type() == null ? null : search.type().code();
        List<FolderEntity> found;
        if (!Strings.isBlank(search.name())) {
            found = jpa.findByNamePrefix(search.name().trim(), type);
        } else if (!Strings.isBlank(search.number())) {
            found = jpa.findByNumberPrefix(search.number().trim(), type);
        } else if (!Strings.isBlank(search.customerName())) {
            found = jpa.findByCustomerNamePrefix(search.customerName().trim(), type);
        } else {
            found = jpa.findAllOfType(type);
        }
        return found.stream().map(FolderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Folder> findHandledBy(Collection<String> handlers) {
        return jpa.findByHandledByInOrderByName(handlers).stream().map(FolderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Folder save(Folder folder) {
        FolderEntity entity = folder.isNew()
                ? new FolderEntity()
                : jpa.findById(toInt(folder.id().value())).orElseThrow();
        FolderDetails d = folder.details();
        entity.type = d.type().code();
        entity.date = d.openingDate();
        entity.name = d.name();
        entity.path = d.path();
        entity.status = d.status();
        entity.firstDate = d.firstDate();
        entity.handledBy = d.handledBy();
        entity.comment = d.comment();
        entity.wayToOurOffice = d.wayToOurOffice();
        entity = jpa.saveAndFlush(entity);
        if (entity.number == null) {
            // the folder number is its identifier (legacy rule)
            entity.number = entity.id;
            entity = jpa.saveAndFlush(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void delete(FolderId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static Folder toDomain(FolderEntity e) {
        return Folder.restore(FolderId.of(e.id), e.number == null ? null : e.number.longValue(),
                new FolderDetails(FolderType.fromCode(e.type), e.date, e.name, e.path, e.status, e.firstDate,
                        e.handledBy, e.comment, e.wayToOurOffice));
    }
}
