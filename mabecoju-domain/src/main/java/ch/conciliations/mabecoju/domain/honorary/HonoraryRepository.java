package ch.conciliations.mabecoju.domain.honorary;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface HonoraryRepository {

    Optional<Honorary> findById(HonoraryId id);

    /** Honoraries of a folder in chronological order. */
    List<Honorary> findByFolder(FolderId folderId);

    List<Honorary> findNotInvoicedByFolder(FolderId folderId);

    List<Honorary> findByInvoice(InvoiceId invoiceId);

    List<Honorary> findBetween(Period period);

    /** Sum of positive, not yet invoiced honoraries per folder. */
    Map<FolderId, Money> sumNotInvoicedByFolder();

    Honorary save(Honorary honorary);

    void delete(HonoraryId id);
}
