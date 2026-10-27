package io.lexora.domain.ticket;

import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.InvoiceId;

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
