package io.lexora.application.invoice;

import io.lexora.domain.advance.Advance;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.honorary.Honorary;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;

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
