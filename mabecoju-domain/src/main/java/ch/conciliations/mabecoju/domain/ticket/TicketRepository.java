package ch.conciliations.mabecoju.domain.ticket;

import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> findById(TicketId id);

    Optional<Ticket> findByNumber(String number);

    List<Ticket> findByFolder(FolderId folderId);

    /** Slips issued for the invoice. */
    List<Ticket> findByInvoice(InvoiceId invoiceId);

    /** Slips issued for the advance request. */
    List<Ticket> findByRequest(AdvanceRequestId requestId);

    Ticket save(Ticket ticket);

    void delete(TicketId id);
}
