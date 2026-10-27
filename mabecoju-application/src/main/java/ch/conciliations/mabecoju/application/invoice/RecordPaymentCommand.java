package ch.conciliations.mabecoju.application.invoice;

import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

public record RecordPaymentCommand(CustomerId customerId, LocalDate date, Money amount, PaymentMeans means) {
}
