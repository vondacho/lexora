package io.lexora.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    /** Same results as the legacy floor((floor(v * 100) + 2) * 0.2) * 0.05. */
    @ParameterizedTest
    @CsvSource({
            "12.30, 12.30",
            "12.32, 12.30",
            "12.33, 12.35",
            "12.37, 12.35",
            "12.38, 12.40",
            "12.329, 12.30",
            "0, 0.00"})
    void rounds_to_5_centimes_like_the_legacy_application(String raw, String expected) {
        assertThat(Money.roundTo5Centimes(new BigDecimal(raw))).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest
    @CsvSource({"73.14615, 73.10", "73.19, 73.10", "73.20, 73.20", "0.09, 0.00"})
    void truncates_to_10_centimes_like_the_legacy_application(String raw, String expected) {
        assertThat(Money.floorTo10Centimes(new BigDecimal(raw))).isEqualTo(Money.of(expected));
    }

    @Test
    void keeps_two_decimals() {
        assertThat(Money.of("10.005").amount()).isEqualByComparingTo("10.01");
        assertThat(Money.of("1").plus(Money.of("0.1"))).isEqualTo(Money.of("1.10"));
    }

    @Test
    void computes_tax_included_in_an_amount() {
        assertThat(Rate.of("0.077").includedIn(Money.of("107.70"))).isEqualTo(Money.of("7.70"));
        assertThat(Rate.ofPercent(new BigDecimal("7.7"))).isEqualTo(Rate.of("0.077"));
    }
}
