package ch.conciliations.mabecoju.domain.billing;

import ch.conciliations.mabecoju.domain.shared.Money;

/** Rule deciding when a debt is considered fully paid. */
public final class Settlement {

    /** A debt is settled when no more than one franc remains to be paid (legacy tolerance). */
    public static final Money TOLERANCE = Money.of("1.00");

    private Settlement() {
    }

    public static boolean isCovered(Money due, Money paid) {
        return paid.isGreaterThanOrEqualTo(due.minus(TOLERANCE));
    }
}
