package ch.conciliations.mabecoju.domain.folder;

import java.util.List;
import java.util.Optional;

public interface FolderStatusRepository {

    List<FolderStatus> findAll();

    Optional<FolderStatus> findByName(String name);

    FolderStatus add(String name);

    void delete(long id);
}
