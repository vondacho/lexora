package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.application.common.NotFoundException;
import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.billing.Recall;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.Folder;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.invoice.Invoice;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.ticket.Ticket;
import ch.conciliations.mabecoju.domain.ticket.TicketId;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Following up what customers owe and recording what they pay. */
@Transactional
public class DebtorUseCases {

    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final HonoraryRepository honoraries;
    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final TicketRepository tickets;
    private final Clock clock;

    public DebtorUseCases(InvoiceRepository invoices, AdvanceRequestRepository requests, AdvanceRepository advances,
                          HonoraryRepository honoraries, CustomerRepository customers, FolderRepository folders,
                          TicketRepository tickets, Clock clock) {
        this.invoices = invoices;
        this.requests = requests;
        this.advances = advances;
        this.honoraries = honoraries;
        this.customers = customers;
        this.folders = folders;
        this.tickets = tickets;
        this.clock = clock;
    }

    /** Open invoices and advance requests of the folder on which the customer still owes something. */
    public CustomerStatement statement(FolderId folderId, CustomerId customerId) {
        Folder folder = Ids.require(folders.findById(folderId), "Folder", folderId.value());
        Customer customer = Ids.require(customers.findById(customerId), "Customer", customerId.value());
        if (!customer.folderId().equals(folderId)) {
            throw new DomainException(ErrorCode.NOT_A_PARTY, "Customer " + customerId.value() + " is not a party of folder " + folderId.value())
                    .with("customerId", customerId.value())
                    .with("folderId", folderId.value());
        }
        List<OpenItem> openInvoices = invoices.findByFolder(folderId).stream()
                .filter(invoice -> !invoice.isPaid())
                .map(invoice -> new OpenItem(invoice.id().value(), invoice.date(), invoice.dueDate(),
                        invoice.shareOf(customerId), invoice.paidSumBy(customerId), invoice.remainingSumFor(customerId)))
                .filter(item -> item.remaining().isPositive())
                .toList();
        List<OpenItem> openRequests = requests.findByFolder(folderId).stream()
                .filter(request -> !request.isPaid())
                .map(request -> {
                    List<Advance> received = advances.findByRequest(request.id());
                    return new OpenItem(request.id().value(), request.date(), request.dueDate(),
                            request.shareOf(customerId), request.paidSumBy(customerId, received),
                            request.remainingSumFor(customerId, received));
                })
                .filter(item -> item.remaining().isPositive())
                .toList();
        return new CustomerStatement(folder, customer, openInvoices, openRequests);
    }

    /**
     * Records what a customer paid, spread over invoices (as invoice payments) and advance requests (as advances).
     * Lines with a zero amount are ignored. Invoices and requests get settled when fully covered.
     */
    public CustomerStatement receivePayments(ReceivePaymentsCommand command) {
        CustomerStatement before = statement(command.folderId(), command.customerId());
        TicketId ticketId = command.ticketId();
        if (ticketId != null) {
            Ids.require(tickets.findById(ticketId), "Ticket", ticketId.value());
        }
        for (ReceivePaymentsCommand.InvoiceLine line : command.invoices()) {
            if (line.amount() == null || !line.amount().isPositive()) {
                continue;
            }
            Invoice invoice = Ids.require(invoices.findById(line.invoiceId()), "Invoice", line.invoiceId().value());
            requireSameFolder(invoice.folderId(), command.folderId(), "Invoice " + invoice.id().value());
            invoice.recordPayment(command.customerId(), line.date(), line.amount(),
                    new PaymentMeans(ticketId, line.iban(), line.cash()));
            invoices.save(invoice);
        }
        for (ReceivePaymentsCommand.RequestLine line : command.requests()) {
            if (line.amount() == null || !line.amount().isPositive()) {
                continue;
            }
            AdvanceRequest request = Ids.require(requests.findById(line.requestId()), "Advance request", line.requestId().value());
            requireSameFolder(request.folderId(), command.folderId(), "Advance request " + request.id().value());
            advances.save(Advance.receive(command.folderId(), request.id(), command.customerId(), line.date(),
                    line.amount(), new PaymentMeans(ticketId, line.iban(), line.cash())));
            request.settleIfCovered(advances.findByRequest(request.id()));
            requests.save(request);
        }
        return statement(before.folder().id(), before.customer().id());
    }

    /** Folders with honoraries to invoice, unpaid invoices and unpaid advance requests. */
    public DebtorsOverview overview() {
        LocalDate today = LocalDate.now(clock);
        Map<FolderId, Money> toBeInvoiced = honoraries.sumNotInvoicedByFolder();
        Map<FolderId, Money> available = advances.sumNotInvoicedByFolder();
        List<Invoice> openInvoices = invoices.findOpen();
        List<AdvanceRequest> openRequests = requests.findOpen();

        Set<FolderId> folderIds = new HashSet<>(toBeInvoiced.keySet());
        openInvoices.forEach(i -> folderIds.add(i.folderId()));
        openRequests.forEach(r -> folderIds.add(r.folderId()));
        Map<FolderId, Folder> byId = folders.findAllById(folderIds);
        Function<FolderId, String> nameOf = id -> byId.containsKey(id) ? byId.get(id).name() : null;

        List<DebtorsOverview.FolderToInvoice> foldersToInvoice = toBeInvoiced.entrySet().stream()
                .map(e -> new DebtorsOverview.FolderToInvoice(e.getKey(), nameOf.apply(e.getKey()), e.getValue(),
                        available.getOrDefault(e.getKey(), Money.ZERO)))
                .sorted(Comparator.comparing(f -> f.folderName() == null ? "" : f.folderName()))
                .toList();

        List<DebtorsOverview.OpenDebt> invoiceDebts = openInvoices.stream()
                .map(i -> new DebtorsOverview.OpenDebt(i.id().value(), i.folderId(), nameOf.apply(i.folderId()),
                        i.date(), i.dueDate(), i.daysOverdue(today), i.amount(), i.remainingSum(),
                        i.recalls().size(), i.lastRecall().map(Recall::date).orElse(null)))
                .toList();

        List<DebtorsOverview.OpenDebt> requestDebts = new ArrayList<>();
        for (AdvanceRequest r : openRequests) {
            List<Advance> received = advances.findByRequest(r.id());
            requestDebts.add(new DebtorsOverview.OpenDebt(r.id().value(), r.folderId(), nameOf.apply(r.folderId()),
                    r.date(), r.dueDate(), r.daysOverdue(today), r.amount(), r.remainingSum(received),
                    r.recalls().size(), r.lastRecall().map(Recall::date).orElse(null)));
        }
        return new DebtorsOverview(foldersToInvoice, invoiceDebts, requestDebts);
    }

    /** Finds the debtor of a payment slip from its typed or scanned reference. */
    public TicketLookup lookupTicket(String reference) {
        String number = Ticket.decodeReference(reference);
        Ticket ticket = tickets.findByNumber(number).orElseThrow(() -> new NotFoundException("Ticket", number));
        Folder folder = ticket.folderId() == null ? null : folders.findById(ticket.folderId()).orElse(null);
        Customer customer = ticket.customerId() == null ? null : customers.findById(ticket.customerId()).orElse(null);
        CustomerStatement statement = folder != null && customer != null && customer.folderId().equals(folder.id())
                ? statement(folder.id(), customer.id())
                : null;
        return new TicketLookup(ticket, folder, customer, invoices.findPaymentsByTicket(ticket.id()),
                advances.findByTicket(ticket.id()), statement);
    }

    private static void requireSameFolder(FolderId actual, FolderId expected, String what) {
        if (!actual.equals(expected)) {
            throw new DomainException(ErrorCode.OTHER_FOLDER, what + " belongs to another folder")
                    .with("document", what);
        }
    }
}
