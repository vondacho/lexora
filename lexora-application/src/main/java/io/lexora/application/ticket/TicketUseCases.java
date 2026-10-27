package io.lexora.application.ticket;

import io.lexora.application.common.Ids;
import io.lexora.domain.advance.AdvanceRepository;
import io.lexora.domain.advance.AdvanceRequest;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.advance.AdvanceRequestRepository;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.customer.CustomerRepository;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.invoice.Invoice;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.invoice.InvoiceRepository;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.ticket.Ticket;
import io.lexora.domain.ticket.TicketId;
import io.lexora.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/** Managing the payment slips handed to the customers. */
@Transactional
public class TicketUseCases {

    private final TicketRepository tickets;
    private final FolderRepository folders;
    private final CustomerRepository customers;
    private final AdvanceRepository advances;
    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;

    public TicketUseCases(TicketRepository tickets, FolderRepository folders, CustomerRepository customers,
                          AdvanceRepository advances, InvoiceRepository invoices, AdvanceRequestRepository requests) {
        this.tickets = tickets;
        this.folders = folders;
        this.customers = customers;
        this.advances = advances;
        this.invoices = invoices;
        this.requests = requests;
    }

    /**
     * Registers a payment slip, optionally issued for an invoice or an advance request of the folder. The reference
     * may be typed or scanned. An already known reference is returned as is, except for cash tickets which are
     * always created.
     */
    public Ticket registerTicket(String reference, FolderId folderId, CustomerId customerId, InvoiceId invoiceId,
                                 AdvanceRequestId requestId) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        if (customerId != null) {
            Customer customer = Ids.require(customers.findById(customerId), "Customer", customerId.value());
            if (!customer.folderId().equals(folderId)) {
                throw new DomainException(ErrorCode.NOT_A_PARTY, "Customer " + customerId.value() + " is not a party of folder " + folderId.value())
                        .with("customerId", customerId.value())
                        .with("folderId", folderId.value());
            }
        }
        if (invoiceId != null) {
            Invoice invoice = Ids.require(invoices.findById(invoiceId), "Invoice", invoiceId.value());
            requireFolder(invoice.folderId(), folderId, "Invoice " + invoiceId.value());
        }
        if (requestId != null) {
            AdvanceRequest request = Ids.require(requests.findById(requestId), "Advance request", requestId.value());
            requireFolder(request.folderId(), folderId, "Advance request " + requestId.value());
        }
        Ticket ticket = Ticket.issue(Ticket.decodeReference(reference), folderId, customerId, invoiceId, requestId);
        if (!ticket.isCash()) {
            Optional<Ticket> known = tickets.findByNumber(ticket.number());
            if (known.isPresent()) {
                return known.get();
            }
        }
        return tickets.save(ticket);
    }

    private static void requireFolder(FolderId actual, FolderId expected, String what) {
        if (!actual.equals(expected)) {
            throw new DomainException(ErrorCode.OTHER_FOLDER, what + " belongs to another folder")
                    .with("document", what);
        }
    }

    public Ticket renumberTicket(TicketId id, String number) {
        Ticket ticket = getTicket(id);
        ticket.renumber(number);
        return tickets.save(ticket);
    }

    /** Deletes a ticket that no payment nor advance refers to. */
    public void deleteTicket(TicketId id) {
        getTicket(id);
        if (isUsed(id)) {
            throw new DomainException(ErrorCode.TICKET_USED, "Ticket " + id.value() + " is used by a payment or an advance")
                    .with("ticketId", id.value());
        }
        tickets.delete(id);
    }

    public boolean isUsed(TicketId id) {
        return !advances.findByTicket(id).isEmpty() || !invoices.findPaymentsByTicket(id).isEmpty();
    }

    public Ticket getTicket(TicketId id) {
        return Ids.require(tickets.findById(id), "Ticket", id.value());
    }

    public List<Ticket> listTickets(FolderId folderId) {
        return tickets.findByFolder(folderId);
    }

    public Optional<Ticket> findByReference(String reference) {
        return tickets.findByNumber(Ticket.decodeReference(reference));
    }
}
