package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Rate;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @param date         due or booking date
 * @param number       voucher number
 * @param ticketNumber payment slip reference of the creditor
 * @param amount       amount including VAT
 * @param taxRate      VAT rate included in the amount
 * @param paidOn       null while not yet paid
 */
public record OutgoingPaymentDetails(
        PaymentKind kind,
        LocalDate date,
        String number,
        String creditor,
        String title,
        String ticketNumber,
        Money amount,
        Rate taxRate,
        LocalDate paidOn) {

    public OutgoingPaymentDetails {
        Objects.requireNonNull(kind, "kind");
        amount = amount == null ? Money.ZERO : amount;
        taxRate = taxRate == null ? Rate.ZERO : taxRate;
    }
}
