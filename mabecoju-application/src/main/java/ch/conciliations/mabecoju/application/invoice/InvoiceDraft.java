package ch.conciliations.mabecoju.application.invoice;

import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.honorary.Honorary;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Rate;

import java.util.List;

/**
 * What the next invoice of a folder would contain, with the proposed defaults.
 */
public record InvoiceDraft(
        FolderId folderId,
        List<Honorary> honoraries,
        List<Advance> advances,
        Money honorariesSum,
        Money advancedSum,
        List<Customer> customers,
        int defaultDelayDays,
        Rate defaultTaxRate) {
}
