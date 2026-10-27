package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Rate;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Terms chosen when issuing or revising an invoice.
 *
 * @param delayDays    payment term in days
 * @param discountRate informative discount rate
 * @param discount     discount amount, deducted before tax
 * @param taxRate      VAT rate
 * @param distribution share of the invoice per customer
 */
public record InvoiceTerms(
        LocalDate date,
        int delayDays,
        Rate discountRate,
        Money discount,
        Rate taxRate,
        Distribution distribution,
        String comment) {

    public InvoiceTerms {
        Objects.requireNonNull(date, "date");
        if (delayDays < 0) {
            throw new DomainException(ErrorCode.NEGATIVE_VALUE, "Payment delay cannot be negative")
                    .with("field", "delay");
        }
        discountRate = discountRate == null ? Rate.ZERO : discountRate;
        discount = discount == null ? Money.ZERO : discount;
        taxRate = taxRate == null ? Rate.ZERO : taxRate;
        distribution = distribution == null ? Distribution.NONE : distribution;
    }
}
