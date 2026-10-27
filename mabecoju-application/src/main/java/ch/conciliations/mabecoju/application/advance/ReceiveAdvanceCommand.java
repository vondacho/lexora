package ch.conciliations.mabecoju.application.advance;

import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Money;

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
