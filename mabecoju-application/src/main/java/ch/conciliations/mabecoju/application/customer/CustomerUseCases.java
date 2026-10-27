package ch.conciliations.mabecoju.application.customer;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerDetails;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Managing the parties (customers) of a folder. */
@Transactional
public class CustomerUseCases {

    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final TicketRepository tickets;

    public CustomerUseCases(CustomerRepository customers, FolderRepository folders, InvoiceRepository invoices,
                            AdvanceRequestRepository requests, AdvanceRepository advances, TicketRepository tickets) {
        this.customers = customers;
        this.folders = folders;
        this.invoices = invoices;
        this.requests = requests;
        this.advances = advances;
        this.tickets = tickets;
    }

    public Customer addCustomer(FolderId folderId, CustomerDetails details) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        return customers.save(Customer.register(folderId, details));
    }

    public Customer updateCustomer(CustomerId id, CustomerDetails details) {
        Customer customer = getCustomer(id);
        customer.update(details);
        return customers.save(customer);
    }

    /**
     * Removes a customer entered by mistake. One with a history in the folder (a share of an invoice or request, a
     * payment, an advance or a payment slip) is kept: end his mandate instead (active until).
     */
    public void removeCustomer(CustomerId id) {
        Customer customer = getCustomer(id);
        FolderId folderId = customer.folderId();
        boolean used = invoices.findByFolder(folderId).stream().anyMatch(i ->
                        i.distribution().shares().containsKey(id)
                                || i.payments().stream().anyMatch(p -> id.equals(p.customerId())))
                || requests.findByFolder(folderId).stream().anyMatch(r -> r.distribution().shares().containsKey(id))
                || advances.findByFolder(folderId).stream().anyMatch(a -> id.equals(a.customerId()))
                || tickets.findByFolder(folderId).stream().anyMatch(t -> id.equals(t.customerId()));
        if (used) {
            throw new DomainException(ErrorCode.CUSTOMER_HAS_HISTORY, "Customer " + id.value() + " has invoices, requests or payments in the folder;"
                    + " end the mandate (active until) instead of removing the customer")
                            .with("customerId", id.value());
        }
        customers.delete(id);
    }

    public Customer getCustomer(CustomerId id) {
        return Ids.require(customers.findById(id), "Customer", id.value());
    }

    public List<Customer> listCustomers(FolderId folderId) {
        return customers.findByFolder(folderId);
    }

    /** Customers who receive the documents dated on the given day. */
    public List<Customer> listActiveCustomers(FolderId folderId, LocalDate date) {
        return customers.findByFolder(folderId).stream().filter(c -> c.isActiveAt(date)).toList();
    }
}
