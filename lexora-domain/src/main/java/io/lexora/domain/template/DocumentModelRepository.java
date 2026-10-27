package io.lexora.domain.template;

import java.util.List;
import java.util.Optional;

public interface DocumentModelRepository {

    Optional<DocumentModel> findById(DocumentModelId id);

    Optional<DocumentModel> findByName(String name);

    /** Models ordered by name. */
    List<DocumentModel> findAll();

    List<DocumentModel> findByType(DocumentType type);

    DocumentModel save(DocumentModel model);

    void delete(DocumentModelId id);
}
