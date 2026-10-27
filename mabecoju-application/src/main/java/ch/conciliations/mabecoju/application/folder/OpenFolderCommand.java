package ch.conciliations.mabecoju.application.folder;

import ch.conciliations.mabecoju.domain.customer.CustomerDetails;
import ch.conciliations.mabecoju.domain.folder.FolderDetails;

/**
 * @param firstCustomer first party of the folder, optional
 */
public record OpenFolderCommand(FolderDetails folder, CustomerDetails firstCustomer) {
}
