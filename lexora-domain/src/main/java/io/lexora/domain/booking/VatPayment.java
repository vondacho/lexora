package io.lexora.domain.booking;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import io.lexora.domain.shared.Rate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * VAT paid to the tax administration for a period ({@code book_vat_payments}), with the figures it was computed from:
 * the money received in the period and the net tax rate at the time of payment.
 * The money leaving the office is also booked as an office expense ({@link #expenseDetails()}), linked to the payment:
 * it counts in the expenses and the balance, and goes away with the VAT payment.
 */
public final class VatPayment {

    /** Creditor of the expense booked with each VAT payment. */
    public static final String TAX_ADMINISTRATION = "Administration fédérale des contributions";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final VatPaymentId id;
    private final Period period;
    private final Money revenue;
    private final Rate rate;
    private final Money amount;
    private final LocalDate paidOn;
    /** The office expense recording the money paid; null for payments recorded before expenses were booked. */
    private OutgoingPaymentId expenseId;

    private VatPayment(VatPaymentId id, Period period, Money revenue, Rate rate, Money amount, LocalDate paidOn,
                       OutgoingPaymentId expenseId) {
        this.id = id;
        this.period = Objects.requireNonNull(period, "period");
        this.revenue = Objects.requireNonNull(revenue, "revenue");
        this.rate = Objects.requireNonNull(rate, "rate");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.paidOn = Objects.requireNonNull(paidOn, "paidOn");
        this.expenseId = expenseId;
    }

    /**
     * @param revenue money received in the period, VAT included
     * @param amount  what was actually paid; usually {@link #vatDue()}, but the return may have been corrected
     */
    public static VatPayment record(Period period, Money revenue, Rate rate, Money amount, LocalDate paidOn) {
        VatPayment payment = new VatPayment(null, period, revenue, rate, amount, paidOn, null);
        if (revenue.isNegative() || amount.isNegative()) {
            throw new DomainException(ErrorCode.NEGATIVE_VALUE, "A VAT payment cannot be negative")
                    .with("field", "amount");
        }
        if (paidOn.isBefore(period.from())) {
            throw new DomainException(ErrorCode.VAT_PAID_BEFORE_PERIOD, "The VAT of a period cannot be paid before the period starts");
        }
        return payment;
    }

    public static VatPayment restore(VatPaymentId id, Period period, Money revenue, Rate rate, Money amount,
                                     LocalDate paidOn, OutgoingPaymentId expenseId) {
        return new VatPayment(Objects.requireNonNull(id, "id"), period, revenue, rate, amount, paidOn, expenseId);
    }

    /** The office expense to book for this payment: paid to the tax administration, without VAT of its own. */
    public OutgoingPaymentDetails expenseDetails() {
        return new OutgoingPaymentDetails(PaymentKind.BANK, paidOn, null, TAX_ADMINISTRATION,
                "TVA du " + DAY.format(period.from()) + " au " + DAY.format(period.to()), null, amount, Rate.ZERO,
                paidOn);
    }

    /** Links the expense booked for this payment. */
    public void bookedAs(OutgoingPaymentId expense) {
        if (expenseId != null) {
            throw new DomainException(ErrorCode.VAT_ALREADY_BOOKED, "The VAT payment is already booked as expense " + expenseId.value());
        }
        this.expenseId = Objects.requireNonNull(expense, "expense");
    }

    public OutgoingPaymentId expenseId() {
        return expenseId;
    }

    /** VAT owed according to the figures: revenue × rate. */
    public Money vatDue() {
        return Money.of(rate.applyTo(revenue));
    }

    public boolean overlaps(Period other) {
        return !period.to().isBefore(other.from()) && !other.to().isBefore(period.from());
    }

    public VatPaymentId id() {
        return id;
    }

    public Period period() {
        return period;
    }

    public Money revenue() {
        return revenue;
    }

    public Rate rate() {
        return rate;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate paidOn() {
        return paidOn;
    }
}
