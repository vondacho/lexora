package io.lexora.domain.template;

import java.util.Objects;

/** A letter template used to print invoices, advance requests and recalls. */
public final class DocumentModel {

    private final DocumentModelId id;
    private DocumentModelDetails details;

    private DocumentModel(DocumentModelId id, DocumentModelDetails details) {
        this.id = id;
        this.details = Objects.requireNonNull(details, "details");
    }

    public static DocumentModel create(DocumentModelDetails details) {
        return new DocumentModel(null, details);
    }

    public static DocumentModel restore(DocumentModelId id, DocumentModelDetails details) {
        return new DocumentModel(Objects.requireNonNull(id, "id"), details);
    }

    public void update(DocumentModelDetails details) {
        this.details = Objects.requireNonNull(details, "details");
    }

    public DocumentModelId id() {
        return id;
    }

    public DocumentModelDetails details() {
        return details;
    }

    public String name() {
        return details.name();
    }
}
