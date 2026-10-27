package io.lexora.application.invoice;

import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.shared.Money;

import java.time.LocalDate;

public record RecordPaymentCommand(CustomerId customerId, LocalDate date, Money amount, PaymentMeans means) {
}
