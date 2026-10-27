package io.lexora.application.reporting;

import io.lexora.domain.folder.Folder;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;

import java.util.List;

/**
 * Activity of the folders a user handles over a period.
 *
 * @param folders         folders whose handler is the user (his shortcut in {@code fold_handled_by})
 * @param honorariesTotal honoraries recorded in the period on those folders
 * @param incomesTotal    advances and invoice payments received in the period on those folders
 */
public record UserStatistics(String username, Period period, List<Folder> folders, Money honorariesTotal,
                             Money incomesTotal) {
}
