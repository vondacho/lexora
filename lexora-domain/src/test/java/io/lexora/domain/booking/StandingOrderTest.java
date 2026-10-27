package io.lexora.domain.booking;

import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StandingOrderTest {

    @Test
    void executes_on_its_day_clamped_to_the_month_length_as_a_paid_permanent_payment() {
        StandingOrder rent = StandingOrder.create(null,
                new StandingOrderDetails("Landlord", "Rent", Money.of("2154.00"), Rate.of("0.077"), 31, true));

        OutgoingPayment payment = rent.executeFor(YearMonth.of(2025, 2)).orElseThrow();

        assertThat(payment.details().kind()).isEqualTo(PaymentKind.PERMANENT);
        assertThat(payment.details().date()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(payment.details().paidOn()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(payment.amount()).isEqualTo(Money.of("2154.00"));
        assertThat(payment.tax()).isEqualTo(Money.of("154.00"));
    }

    @Test
    void a_disabled_order_produces_nothing() {
        StandingOrder order = StandingOrder.create(null,
                new StandingOrderDetails("Telco", "Phone", Money.of("80"), Rate.ZERO, 5, false));

        assertThat(order.executeFor(YearMonth.of(2025, 2))).isEmpty();
    }

    @Test
    void next_execution_is_the_month_after_the_last_one() {
        YearMonth current = YearMonth.of(2026, 10);

        assertThat(StandingOrder.nextExecutionMonth(Optional.of(LocalDate.of(2025, 12, 5)), current))
                .isEqualTo(YearMonth.of(2026, 1));
        assertThat(StandingOrder.nextExecutionMonth(Optional.empty(), current)).isEqualTo(current);
    }
}
