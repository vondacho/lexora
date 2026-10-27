package io.lexora.domain.advance;

import io.lexora.domain.folder.FolderId;

import java.util.List;
import java.util.Optional;

public interface AdvanceRequestRepository {

    Optional<AdvanceRequest> findById(AdvanceRequestId id);

    /** Requests of a folder, most recent first. */
    List<AdvanceRequest> findByFolder(FolderId folderId);

    /** Unpaid requests, oldest first. */
    List<AdvanceRequest> findOpen();

    AdvanceRequest save(AdvanceRequest request);

    void delete(AdvanceRequestId id);
}
