package io.lexora.domain.customer;

import io.lexora.domain.folder.FolderId;
import io.lexora.domain.shared.Strings;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A party of a folder: the person or couple who is invoiced. */
public final class Customer {

    private final CustomerId id;
    private final FolderId folderId;
    private CustomerDetails details;

    private Customer(CustomerId id, FolderId folderId, CustomerDetails details) {
        this.id = id;
        this.folderId = Objects.requireNonNull(folderId, "folderId");
        this.details = validate(details);
    }

    public static Customer register(FolderId folderId, CustomerDetails details) {
        return new Customer(null, folderId, details);
    }

    public static Customer restore(CustomerId id, FolderId folderId, CustomerDetails details) {
        return new Customer(Objects.requireNonNull(id, "id"), folderId, details);
    }

    public void update(CustomerDetails details) {
        this.details = validate(details);
    }

    private static CustomerDetails validate(CustomerDetails details) {
        Objects.requireNonNull(details, "details");
        if (Strings.isBlank(details.lastName()) && Strings.isBlank(details.firstName())) {
            Strings.requireNotBlank(details.lastName(), "Customer name");
        }
        return details;
    }

    public boolean isActiveAt(LocalDate date) {
        ActivityPeriod activity = details.activity();
        return activity == null || activity.includes(date);
    }

    public String fullName() {
        return Stream.of(details.firstName(), details.lastName())
                .filter(s -> !Strings.isBlank(s))
                .collect(Collectors.joining(" "));
    }

    public CustomerId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public CustomerDetails details() {
        return details;
    }

    public boolean isBilling() {
        return details.billing();
    }
}
