package io.lexora.application.advance;

import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.shared.Money;

import java.time.LocalDate;

/**
 * @param requestId  request the advance answers, null for a spontaneous advance
 * @param customerId paying customer, if known
 */
public record ReceiveAdvanceCommand(
        FolderId folderId,
        AdvanceRequestId requestId,
        CustomerId customerId,
        LocalDate date,
        Money amount,
        PaymentMeans means) {
}
