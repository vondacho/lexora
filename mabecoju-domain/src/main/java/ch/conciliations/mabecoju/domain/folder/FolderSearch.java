package ch.conciliations.mabecoju.domain.folder;

/**
 * Folder search criteria. Text criteria match by prefix. When all of them are blank,
 * every folder of the requested type is returned.
 *
 * @param type null for any type
 */
public record FolderSearch(String name, String number, String customerName, FolderType type) {

    public static FolderSearch all() {
        return new FolderSearch(null, null, null, null);
    }
}
