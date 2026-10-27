package io.lexora.domain.customer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityPeriodTest {

    private static final LocalDate DAY = LocalDate.of(2026, 6, 15);

    @Test
    void follows_the_legacy_active_customer_rule() {
        assertThat(ActivityPeriod.ALWAYS.includes(DAY)).isTrue();
        assertThat(new ActivityPeriod(DAY, null).includes(DAY)).isTrue();
        assertThat(new ActivityPeriod(DAY.plusDays(1), null).includes(DAY)).isFalse();
        assertThat(new ActivityPeriod(DAY.minusYears(1), DAY).includes(DAY)).isTrue();
        assertThat(new ActivityPeriod(DAY.minusYears(1), DAY.minusDays(1)).includes(DAY)).isFalse();
    }
}
