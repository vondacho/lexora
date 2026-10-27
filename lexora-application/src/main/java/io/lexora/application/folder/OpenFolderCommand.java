package io.lexora.application.folder;

import io.lexora.domain.customer.CustomerDetails;
import io.lexora.domain.folder.FolderDetails;

/**
 * @param firstCustomer first party of the folder, optional
 */
public record OpenFolderCommand(FolderDetails folder, CustomerDetails firstCustomer) {
}
