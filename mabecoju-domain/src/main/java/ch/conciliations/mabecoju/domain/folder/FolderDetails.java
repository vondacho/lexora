package ch.conciliations.mabecoju.domain.folder;

import java.time.LocalDate;

/**
 * Descriptive data of a folder.
 *
 * @param openingDate    date the folder was opened ({@code fold_date})
 * @param firstDate      date of the first meeting ({@code fold_first_date})
 * @param handledBy      shortcut of the person in charge
 * @param status         free text status, usually one of the {@link FolderStatus} names
 * @param wayToOurOffice how the clients found the office
 */
public record FolderDetails(
        FolderType type,
        LocalDate openingDate,
        String name,
        String path,
        String status,
        LocalDate firstDate,
        String handledBy,
        String comment,
        String wayToOurOffice) {
}
