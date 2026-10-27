package ch.conciliations.mabecoju.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A rate expressed as a fraction (0.077 for 7.7 %), the way the legacy schema stores
 * VAT, discount and net tax rates.
 */
public record Rate(BigDecimal value) {

    public static final Rate ZERO = new Rate(BigDecimal.ZERO);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Rate {
        Objects.requireNonNull(value, "value");
        if (value.signum() < 0) {
            throw new DomainException(ErrorCode.NEGATIVE_VALUE, "A rate cannot be negative: " + value)
                    .with("field", "rate");
        }
        value = value.stripTrailingZeros();
    }

    public static Rate of(BigDecimal fraction) {
        return new Rate(fraction);
    }

    public static Rate of(String fraction) {
        return new Rate(new BigDecimal(fraction));
    }

    public static Rate ofNullable(BigDecimal fraction) {
        return fraction == null ? ZERO : new Rate(fraction);
    }

    public static Rate ofPercent(BigDecimal percent) {
        return new Rate(percent.divide(HUNDRED, 6, RoundingMode.HALF_UP));
    }

    public BigDecimal percent() {
        return value.multiply(HUNDRED).stripTrailingZeros();
    }

    /** Raw (unrounded) product of the amount by this rate. */
    public BigDecimal applyTo(Money money) {
        return money.amount().multiply(value);
    }

    /** Share of the amount that is tax when the amount already includes the tax: amount * rate / (1 + rate). */
    public Money includedIn(Money grossAmount) {
        BigDecimal divisor = BigDecimal.ONE.add(value);
        return Money.of(grossAmount.amount().multiply(value).divide(divisor, 2, RoundingMode.HALF_UP));
    }
}
