package ch.conciliations.mabecoju.application.advance;

import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.util.List;

/**
 * Figures shown when preparing an advance request.
 *
 * @param uncoveredHonorariesSum open honoraries not covered by the available advances
 */
public record AdvanceRequestDraft(
        FolderId folderId,
        Money openHonorariesSum,
        Money availableAdvancesSum,
        Money uncoveredHonorariesSum,
        List<Customer> customers,
        int defaultDelayDays) {
}
