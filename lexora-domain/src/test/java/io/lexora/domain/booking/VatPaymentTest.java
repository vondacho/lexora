package io.lexora.domain.booking;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import io.lexora.domain.shared.Rate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VatPaymentTest {

    private static final Period S1 = Period.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    @Test
    void the_vat_due_is_the_revenue_at_the_net_tax_rate() {
        VatPayment payment = VatPayment.record(S1, Money.of("18929.30"), Rate.of("0.059"), Money.of("1116.83"),
                LocalDate.of(2026, 8, 28));

        assertThat(payment.vatDue()).isEqualTo(Money.of("1116.83"));
    }

    @Test
    void detects_overlapping_periods() {
        VatPayment payment = VatPayment.record(S1, Money.of("100"), Rate.of("0.059"), Money.of("5.90"),
                LocalDate.of(2026, 8, 28));

        assertThat(payment.overlaps(Period.of(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31)))).isTrue();
        assertThat(payment.overlaps(Period.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)))).isFalse();
    }

    @Test
    void is_booked_as_a_paid_office_expense_to_the_tax_administration() {
        VatPayment payment = VatPayment.record(S1, Money.of("18929.30"), Rate.of("0.059"), Money.of("1116.80"),
                LocalDate.of(2026, 8, 28));

        OutgoingPaymentDetails expense = payment.expenseDetails();
        assertThat(expense.kind()).isEqualTo(PaymentKind.BANK);
        assertThat(expense.creditor()).isEqualTo("Administration fédérale des contributions");
        assertThat(expense.title()).isEqualTo("TVA du 01.01.2026 au 30.06.2026");
        assertThat(expense.amount()).isEqualTo(Money.of("1116.80"));
        assertThat(expense.taxRate()).isEqualTo(Rate.ZERO);
        assertThat(expense.date()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(expense.paidOn()).isEqualTo(LocalDate.of(2026, 8, 28));

        payment.bookedAs(OutgoingPaymentId.of(7L));
        assertThat(payment.expenseId()).isEqualTo(OutgoingPaymentId.of(7L));
        assertThatThrownBy(() -> payment.bookedAs(OutgoingPaymentId.of(8L))).isInstanceOf(DomainException.class);
    }

    @Test
    void rejects_negative_amounts_and_payments_before_the_period() {
        assertThatThrownBy(() -> VatPayment.record(S1, Money.of("100"), Rate.of("0.059"), Money.of("-1"),
                LocalDate.of(2026, 8, 28))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> VatPayment.record(S1, Money.of("100"), Rate.of("0.059"), Money.of("5.90"),
                LocalDate.of(2025, 12, 31))).isInstanceOf(DomainException.class);
    }
}
