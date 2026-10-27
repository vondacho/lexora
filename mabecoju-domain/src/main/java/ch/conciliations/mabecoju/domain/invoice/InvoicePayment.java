package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

/**
 * Money received from a customer for an invoice.
 *
 * @param id         null until persisted
 * @param customerId null for legacy payments recorded without customer
 */
public record InvoicePayment(PaymentId id, CustomerId customerId, LocalDate date, Money amount, PaymentMeans means) {
}
