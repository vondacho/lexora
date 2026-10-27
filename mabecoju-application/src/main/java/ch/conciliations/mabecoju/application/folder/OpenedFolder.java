package ch.conciliations.mabecoju.application.folder;

import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.folder.Folder;

/** @param customer null when no first customer was given */
public record OpenedFolder(Folder folder, Customer customer) {
}
