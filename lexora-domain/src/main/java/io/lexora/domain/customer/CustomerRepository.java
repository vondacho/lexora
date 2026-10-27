package io.lexora.domain.customer;

import io.lexora.domain.folder.FolderId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(CustomerId id);

    /** Customers of a folder ordered by the start of their activity, as the legacy application did. */
    List<Customer> findByFolder(FolderId folderId);

    Customer save(Customer customer);

    void delete(CustomerId id);
}
