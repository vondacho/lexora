package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.folder.Folder;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.util.List;

/** What a customer still owes on the invoices and advance requests of his folder. */
public record CustomerStatement(Folder folder, Customer customer, List<OpenItem> invoices, List<OpenItem> requests) {

    public Money totalRemaining() {
        return invoices.stream().collect(Money.summing(OpenItem::remaining))
                .plus(requests.stream().collect(Money.summing(OpenItem::remaining)));
    }
}
