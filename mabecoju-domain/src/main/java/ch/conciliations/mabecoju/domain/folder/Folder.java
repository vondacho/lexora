package ch.conciliations.mabecoju.domain.folder;

import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Strings;

import java.util.Objects;

/**
 * A client mandate ("dossier"). Every customer, honorary, advance request, advance, invoice and ticket
 * belongs to exactly one folder.
 */
public final class Folder {

    private final FolderId id;
    private final Long number;
    private FolderDetails details;

    private Folder(FolderId id, Long number, FolderDetails details) {
        this.id = id;
        this.number = number;
        this.details = validate(details);
    }

    /** Opens a new folder; once persisted, its number is its identifier (legacy rule). */
    public static Folder open(FolderDetails details) {
        return new Folder(null, null, details);
    }

    public static Folder restore(FolderId id, Long number, FolderDetails details) {
        return new Folder(Objects.requireNonNull(id, "id"), number, details);
    }

    public void update(FolderDetails details) {
        this.details = validate(details);
    }

    private static FolderDetails validate(FolderDetails details) {
        Objects.requireNonNull(details, "details");
        Strings.requireNotBlank(details.name(), "Folder name");
        Strings.requireMaxLength(details.name(), 50, "Folder name");
        Strings.requireMaxLength(details.handledBy(), 10, "Folder handler");
        if (details.type() == null || details.type() == FolderType.UNDEFINED) {
            throw new DomainException(ErrorCode.FOLDER_TYPE_MANDATORY, "Folder type is mandatory");
        }
        return details;
    }

    public boolean isNew() {
        return id == null;
    }

    public FolderId id() {
        return id;
    }

    public Long number() {
        return number;
    }

    public FolderDetails details() {
        return details;
    }

    public String name() {
        return details.name();
    }

    public FolderType type() {
        return details.type();
    }
}
