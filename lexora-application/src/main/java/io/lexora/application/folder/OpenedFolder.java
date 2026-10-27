package io.lexora.application.folder;

import io.lexora.domain.customer.Customer;
import io.lexora.domain.folder.Folder;

/** @param customer null when no first customer was given */
public record OpenedFolder(Folder folder, Customer customer) {
}
