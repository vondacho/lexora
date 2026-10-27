package io.lexora.application.advance;

import io.lexora.domain.customer.Customer;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.shared.Money;

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
