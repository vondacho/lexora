package io.lexora.application.debtor;

import io.lexora.domain.customer.Customer;
import io.lexora.domain.folder.Folder;
import io.lexora.domain.shared.Money;

import java.util.List;

/** What a customer still owes on the invoices and advance requests of his folder. */
public record CustomerStatement(Folder folder, Customer customer, List<OpenItem> invoices, List<OpenItem> requests) {

    public Money totalRemaining() {
        return invoices.stream().collect(Money.summing(OpenItem::remaining))
                .plus(requests.stream().collect(Money.summing(OpenItem::remaining)));
    }
}
