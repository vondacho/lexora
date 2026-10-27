package io.lexora.domain.invoice;

import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.shared.Money;

import java.time.LocalDate;

/**
 * Money received from a customer for an invoice.
 *
 * @param id         null until persisted
 * @param customerId null for legacy payments recorded without customer
 */
public record InvoicePayment(PaymentId id, CustomerId customerId, LocalDate date, Money amount, PaymentMeans means) {
}
