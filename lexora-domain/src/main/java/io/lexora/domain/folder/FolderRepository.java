package io.lexora.domain.folder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FolderRepository {

    Optional<Folder> findById(FolderId id);

    Map<FolderId, Folder> findAllById(Collection<FolderId> ids);

    List<Folder> search(FolderSearch search);

    /** Folders handled by one of the given handlers ({@code fold_handled_by}, usually a user shortcut), by name. */
    List<Folder> findHandledBy(Collection<String> handlers);

    Folder save(Folder folder);

    void delete(FolderId id);
}
