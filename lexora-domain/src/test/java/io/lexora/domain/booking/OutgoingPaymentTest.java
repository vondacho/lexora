package io.lexora.domain.booking;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutgoingPaymentTest {

    private static OutgoingPaymentDetails expense(String amount, String rate) {
        return new OutgoingPaymentDetails(PaymentKind.EXPENSE, LocalDate.of(2026, 10, 2), null, "Papeterie",
                "Classeurs", null, Money.of(amount), Rate.of(rate), null);
    }

    @Test
    void a_one_off_expense_can_be_modified_and_its_tax_is_recomputed() {
        OutgoingPayment payment = OutgoingPayment.recordManual(null, expense("54.10", "0"));

        payment.update(expense("108.10", "0.081"));

        assertThat(payment.amount()).isEqualTo(Money.of("108.10"));
        assertThat(payment.tax()).isEqualTo(Money.of("8.10"));
        assertThat(payment.isPermanent()).isFalse();
    }

    @Test
    void a_manual_entry_cannot_claim_to_be_permanent() {
        OutgoingPaymentDetails permanent = new OutgoingPaymentDetails(PaymentKind.PERMANENT, null, null, "Régie",
                "Loyer", null, Money.of("100"), Rate.ZERO, null);

        assertThatThrownBy(() -> OutgoingPayment.recordManual(null, permanent)).isInstanceOf(DomainException.class);
        OutgoingPayment expense = OutgoingPayment.recordManual(null, expense("10", "0"));
        assertThatThrownBy(() -> expense.update(permanent)).isInstanceOf(DomainException.class);
    }

    @Test
    void a_payment_produced_by_a_standing_order_cannot_be_modified() {
        OutgoingPayment executed = StandingOrder.create(null,
                        new StandingOrderDetails("Telco", "Phone", Money.of("80"), Rate.ZERO, 5, true))
                .executeFor(YearMonth.of(2026, 10)).orElseThrow();
        OutgoingPayment stored = OutgoingPayment.restore(OutgoingPaymentId.of(7), null, executed.details(), executed.tax());

        assertThat(stored.isPermanent()).isTrue();
        assertThatThrownBy(() -> stored.update(expense("90", "0")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("standing order");
    }
}
