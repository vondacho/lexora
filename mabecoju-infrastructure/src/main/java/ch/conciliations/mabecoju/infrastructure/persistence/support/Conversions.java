package ch.conciliations.mabecoju.infrastructure.persistence.support;

import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Rate;

import java.math.BigDecimal;

/**
 * Conversions between the legacy column types and the domain types.
 * <p>
 * Amounts are MySQL single precision {@code float}s: going through {@link Float#toString(float)} gives back the
 * decimal value that was written. Missing references were stored either as NULL or as 0.
 */
public final class Conversions {

    private Conversions() {
    }

    public static Money money(Float value) {
        return value == null ? Money.ZERO : Money.of(new BigDecimal(Float.toString(value)));
    }

    public static Money money(Double value) {
        return value == null ? Money.ZERO : Money.of(BigDecimal.valueOf(value));
    }

    public static Float toFloat(Money money) {
        return money == null ? 0f : money.amount().floatValue();
    }

    public static Rate rate(Float value) {
        return value == null ? Rate.ZERO : Rate.of(new BigDecimal(Float.toString(value)));
    }

    public static Float toFloat(Rate rate) {
        return rate == null ? 0f : rate.value().floatValue();
    }

    /** Legacy references use 0 as well as NULL for "none". */
    public static Long reference(Integer value) {
        return value == null || value == 0 ? null : value.longValue();
    }

    public static Integer toInt(long id) {
        return Math.toIntExact(id);
    }

    public static Integer toIntOrNull(Long id) {
        return id == null ? null : Math.toIntExact(id);
    }

    /** For NOT NULL reference columns where the legacy application wrote 0 for "none". */
    public static Integer toIntOrZero(Long id) {
        return id == null ? 0 : Math.toIntExact(id);
    }

    public static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
