package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderType;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.domain.ticket.TicketId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AdvanceRepository {

    Optional<Advance> findById(AdvanceId id);

    /** Advances of a folder, most recent first. */
    List<Advance> findByFolder(FolderId folderId);

    List<Advance> findNotInvoicedByFolder(FolderId folderId);

    List<Advance> findByRequest(AdvanceRequestId requestId);

    List<Advance> findByInvoice(InvoiceId invoiceId);

    List<Advance> findByTicket(TicketId ticketId);

    /** Advances received in the period for folders of the given type. */
    List<Advance> findReceivedBetween(Period period, FolderType folderType);

    List<Advance> findAll();

    /** Sum of positive advances not yet deducted on an invoice, per folder. */
    Map<FolderId, Money> sumNotInvoicedByFolder();

    Advance save(Advance advance);

    void delete(AdvanceId id);
}
