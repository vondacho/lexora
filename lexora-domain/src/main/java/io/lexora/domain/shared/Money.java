package io.lexora.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An amount in Swiss francs, kept with two decimals.
 * <p>
 * The legacy application stored amounts as MySQL {@code float}; this value object brings exact decimal
 * arithmetic and the two Swiss rounding rules the legacy code relied on.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private static final BigDecimal FIVE = BigDecimal.valueOf(5);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public Money {
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    /** Null-tolerant factory: a missing amount means zero, as in the legacy code. */
    public static Money ofNullable(BigDecimal amount) {
        return amount == null ? ZERO : new Money(amount);
    }

    /**
     * Rounds to the nearest 5 centimes after truncating to the centime
     * (legacy {@code MiscHelper::round5Centimes}).
     */
    public static Money roundTo5Centimes(BigDecimal raw) {
        BigDecimal cents = raw.movePointRight(2).setScale(0, RoundingMode.FLOOR);
        BigDecimal fives = cents.add(TWO).divide(FIVE, 0, RoundingMode.FLOOR);
        return new Money(fives.multiply(FIVE).movePointLeft(2));
    }

    /** Truncates down to 10 centimes (legacy {@code MiscHelper::round10Centimes}). */
    public static Money floorTo10Centimes(BigDecimal raw) {
        return new Money(raw.setScale(1, RoundingMode.FLOOR));
    }

    public Money roundTo5Centimes() {
        return roundTo5Centimes(amount);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    /** Negative amounts are brought back to zero. */
    public Money atLeastZero() {
        return isNegative() ? ZERO : this;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    public static Money sum(Collection<Money> amounts) {
        return amounts.stream().reduce(ZERO, Money::plus);
    }

    public static <T> Collector<T, ?, Money> summing(Function<T, Money> mapper) {
        return Collectors.reducing(ZERO, mapper, Money::plus);
    }

    @Override
    public String toString() {
        return "CHF " + amount.toPlainString();
    }
}
