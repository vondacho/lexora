package ch.conciliations.mabecoju.application.invoice;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.honorary.Honorary;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.invoice.Invoice;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.invoice.InvoiceTerms;
import ch.conciliations.mabecoju.domain.invoice.PaymentId;
import ch.conciliations.mabecoju.domain.settings.InvoicingSettings;
import ch.conciliations.mabecoju.domain.settings.SettingsRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Issuing invoices and following their payment. */
@Transactional
public class InvoiceUseCases {

    private final InvoiceRepository invoices;
    private final HonoraryRepository honoraries;
    private final AdvanceRepository advances;
    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final TicketRepository tickets;
    private final SettingsRepository settings;
    private final Clock clock;

    public InvoiceUseCases(InvoiceRepository invoices, HonoraryRepository honoraries, AdvanceRepository advances,
                           CustomerRepository customers, FolderRepository folders, TicketRepository tickets,
                           SettingsRepository settings, Clock clock) {
        this.invoices = invoices;
        this.honoraries = honoraries;
        this.advances = advances;
        this.customers = customers;
        this.folders = folders;
        this.tickets = tickets;
        this.settings = settings;
        this.clock = clock;
    }

    /** Shows what would be invoiced now: open honoraries, available advances and defaults. */
    public InvoiceDraft prepareInvoice(FolderId folderId) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        List<Honorary> open = honoraries.findNotInvoicedByFolder(folderId);
        List<Advance> available = advances.findNotInvoicedByFolder(folderId);
        InvoicingSettings defaults = settings.invoicing();
        LocalDate today = LocalDate.now(clock);
        List<Customer> active = customers.findByFolder(folderId).stream().filter(c -> c.isActiveAt(today)).toList();
        return new InvoiceDraft(folderId, open, available,
                open.stream().collect(Money.summing(Honorary::amount)),
                available.stream().collect(Money.summing(Advance::amount)),
                active, defaults.invoiceDelayDays(), defaults.vatRate());
    }

    /**
     * Invoices every open honorary of the folder and deducts every available advance. Sums are computed from
     * the stored honoraries and advances, never taken from the client.
     */
    public Invoice issueInvoice(FolderId folderId, InvoiceTerms terms) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        checkDistribution(folderId, terms);
        List<Honorary> open = honoraries.findNotInvoicedByFolder(folderId);
        List<Advance> available = advances.findNotInvoicedByFolder(folderId);
        if (open.isEmpty() && available.isEmpty()) {
            throw new DomainException(ErrorCode.NOTHING_TO_INVOICE, "Folder " + folderId.value() + " has nothing to invoice")
                    .with("folderId", folderId.value());
        }
        Invoice invoice = invoices.save(Invoice.issue(folderId,
                open.stream().collect(Money.summing(Honorary::amount)),
                available.stream().collect(Money.summing(Advance::amount)),
                terms));
        open.forEach(h -> {
            h.includeIn(invoice.id());
            honoraries.save(h);
        });
        available.forEach(a -> {
            a.deductOn(invoice.id());
            advances.save(a);
        });
        return invoice;
    }

    public Invoice reviseInvoice(InvoiceId id, InvoiceTerms terms) {
        Invoice invoice = getInvoice(id);
        checkDistribution(invoice.folderId(), terms);
        invoice.revise(terms);
        return invoices.save(invoice);
    }

    /**
     * Deletes the invoice with its payments and recalls; its honoraries and advances become open again, the slips
     * issued for it are kept without the link.
     */
    public void deleteInvoice(InvoiceId id) {
        getInvoice(id);
        tickets.findByInvoice(id).forEach(t -> {
            t.forgetInvoice();
            tickets.save(t);
        });
        honoraries.findByInvoice(id).forEach(h -> {
            h.release();
            honoraries.save(h);
        });
        advances.findByInvoice(id).forEach(a -> {
            a.release();
            advances.save(a);
        });
        invoices.delete(id);
    }

    public Invoice getInvoice(InvoiceId id) {
        return Ids.require(invoices.findById(id), "Invoice", id.value());
    }

    public List<Honorary> invoicedHonoraries(InvoiceId id) {
        return honoraries.findByInvoice(id);
    }

    public List<Advance> deductedAdvances(InvoiceId id) {
        return advances.findByInvoice(id);
    }

    public List<Invoice> listInvoices(FolderId folderId) {
        return invoices.findByFolder(folderId);
    }

    public List<Invoice> listOpenInvoices() {
        return invoices.findOpen();
    }

    public List<Invoice> listPaidInvoices() {
        return invoices.findPaid();
    }

    /** Marks the invoice as paid ("solder"), today when no date is given. */
    public Invoice settleInvoice(InvoiceId id, LocalDate on) {
        Invoice invoice = getInvoice(id);
        invoice.settleOn(on != null ? on : LocalDate.now(clock));
        return invoices.save(invoice);
    }

    public Invoice reopenInvoice(InvoiceId id) {
        Invoice invoice = getInvoice(id);
        invoice.reopen();
        return invoices.save(invoice);
    }

    public Invoice recallInvoice(InvoiceId id, LocalDate on) {
        Invoice invoice = getInvoice(id);
        invoice.recall(on != null ? on : LocalDate.now(clock));
        return invoices.save(invoice);
    }

    public Invoice cancelRecall(InvoiceId id, RecallId recallId) {
        Invoice invoice = getInvoice(id);
        invoice.cancelRecall(recallId);
        return invoices.save(invoice);
    }

    public Invoice recordPayment(InvoiceId id, RecordPaymentCommand command) {
        Invoice invoice = getInvoice(id);
        if (command.means() != null && command.means().ticketId() != null) {
            Ids.require(tickets.findById(command.means().ticketId()), "Ticket", command.means().ticketId().value());
        }
        invoice.recordPayment(command.customerId(), command.date(), command.amount(), command.means());
        return invoices.save(invoice);
    }

    public Invoice cancelPayment(PaymentId paymentId) {
        InvoiceId invoiceId = Ids.require(invoices.findInvoiceOfPayment(paymentId), "Payment", paymentId.value());
        Invoice invoice = getInvoice(invoiceId);
        invoice.cancelPayment(paymentId);
        return invoices.save(invoice);
    }

    private void checkDistribution(FolderId folderId, InvoiceTerms terms) {
        Set<CustomerId> parties = customers.findByFolder(folderId).stream()
                .map(Customer::id)
                .collect(Collectors.toSet());
        terms.distribution().shares().keySet().stream()
                .filter(customer -> !parties.contains(customer))
                .findFirst()
                .ifPresent(customer -> {
                    throw new DomainException(ErrorCode.NOT_A_PARTY, "Customer " + customer.value() + " is not a party of folder " + folderId.value())
                            .with("customerId", customer.value())
                            .with("folderId", folderId.value());
                });
    }
}
