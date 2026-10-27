package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.domain.ticket.TicketId;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    Optional<Invoice> findById(InvoiceId id);

    /** Invoices of a folder, most recent first. */
    List<Invoice> findByFolder(FolderId folderId);

    /** Unpaid invoices, oldest first. */
    List<Invoice> findOpen();

    /** Paid invoices, most recently paid first. */
    List<Invoice> findPaid();

    List<InvoicePaymentRecord> findPaymentsBetween(Period period);

    List<InvoicePaymentRecord> findPaymentsByTicket(TicketId ticketId);

    List<InvoicePaymentRecord> findAllPayments();

    Optional<InvoiceId> findInvoiceOfPayment(PaymentId paymentId);

    Invoice save(Invoice invoice);

    void delete(InvoiceId id);
}
