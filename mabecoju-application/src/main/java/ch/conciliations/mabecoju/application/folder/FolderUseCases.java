package ch.conciliations.mabecoju.application.folder;

import ch.conciliations.mabecoju.application.advance.AdvanceRequestUseCases;
import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.application.invoice.InvoiceUseCases;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentRepository;
import ch.conciliations.mabecoju.domain.booking.StandingOrderRepository;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.Folder;
import ch.conciliations.mabecoju.domain.folder.FolderDetails;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.folder.FolderSearch;
import ch.conciliations.mabecoju.domain.folder.FolderStatus;
import ch.conciliations.mabecoju.domain.folder.FolderStatusRepository;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Strings;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.util.List;

/** Opening, maintaining, searching and closing client folders. */
@Transactional
public class FolderUseCases {

    private final FolderRepository folders;
    private final FolderStatusRepository statuses;
    private final CustomerRepository customers;
    private final HonoraryRepository honoraries;
    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final TicketRepository tickets;
    private final OutgoingPaymentRepository outgoingPayments;
    private final StandingOrderRepository standingOrders;
    private final InvoiceUseCases invoiceUseCases;
    private final AdvanceRequestUseCases requestUseCases;

    public FolderUseCases(FolderRepository folders, FolderStatusRepository statuses, CustomerRepository customers,
                          HonoraryRepository honoraries, InvoiceRepository invoices, AdvanceRequestRepository requests,
                          AdvanceRepository advances, TicketRepository tickets,
                          OutgoingPaymentRepository outgoingPayments, StandingOrderRepository standingOrders,
                          InvoiceUseCases invoiceUseCases, AdvanceRequestUseCases requestUseCases) {
        this.folders = folders;
        this.statuses = statuses;
        this.customers = customers;
        this.honoraries = honoraries;
        this.invoices = invoices;
        this.requests = requests;
        this.advances = advances;
        this.tickets = tickets;
        this.outgoingPayments = outgoingPayments;
        this.standingOrders = standingOrders;
        this.invoiceUseCases = invoiceUseCases;
        this.requestUseCases = requestUseCases;
    }

    /** Opens a folder, numbered after its identifier, optionally with its first customer. */
    public OpenedFolder openFolder(OpenFolderCommand command) {
        Folder folder = folders.save(Folder.open(command.folder()));
        Customer customer = command.firstCustomer() == null
                ? null
                : customers.save(Customer.register(folder.id(), command.firstCustomer()));
        return new OpenedFolder(folder, customer);
    }

    public Folder updateFolder(FolderId id, FolderDetails details) {
        Folder folder = getFolder(id);
        folder.update(details);
        return folders.save(folder);
    }

    /**
     * Deletes a folder and everything belonging to it: invoices (with payments and recalls), advance requests,
     * advances, honoraries, payment slips, booking entries, standing orders and customers.
     */
    public void deleteFolder(FolderId id) {
        getFolder(id);
        invoices.findByFolder(id).forEach(invoice -> invoiceUseCases.deleteInvoice(invoice.id()));
        requests.findByFolder(id).forEach(request -> requestUseCases.deleteRequest(request.id()));
        advances.findByFolder(id).forEach(advance -> advances.delete(advance.id()));
        honoraries.findByFolder(id).forEach(honorary -> honoraries.delete(honorary.id()));
        tickets.findByFolder(id).forEach(ticket -> tickets.delete(ticket.id()));
        outgoingPayments.findByFolder(id).forEach(payment -> outgoingPayments.delete(payment.id()));
        standingOrders.findByFolder(id).forEach(order -> standingOrders.delete(order.id()));
        customers.findByFolder(id).forEach(customer -> customers.delete(customer.id()));
        folders.delete(id);
    }

    public Folder getFolder(FolderId id) {
        return Ids.require(folders.findById(id), "Folder", id.value());
    }

    public List<Folder> searchFolders(FolderSearch search) {
        return folders.search(search == null ? FolderSearch.all() : search);
    }

    public List<FolderStatus> listStatuses() {
        return statuses.findAll();
    }

    public FolderStatus addStatus(String name) {
        String trimmed = Strings.requireNotBlank(name, "Status name");
        Strings.requireMaxLength(trimmed, 80, "Status name");
        if (statuses.findByName(trimmed).isPresent()) {
            throw new DomainException(ErrorCode.FOLDER_STATUS_EXISTS, "Status '" + trimmed + "' already exists")
                    .with("name", trimmed);
        }
        return statuses.add(trimmed);
    }

    public void deleteStatus(long id) {
        statuses.delete(id);
    }
}
