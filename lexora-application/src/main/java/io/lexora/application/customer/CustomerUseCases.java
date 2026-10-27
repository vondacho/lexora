package io.lexora.application.customer;

import io.lexora.application.common.Ids;
import io.lexora.domain.advance.AdvanceRepository;
import io.lexora.domain.advance.AdvanceRequestRepository;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerDetails;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.customer.CustomerRepository;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.invoice.InvoiceRepository;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.ticket.TicketRepository;
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
