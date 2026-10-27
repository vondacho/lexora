package ch.conciliations.mabecoju.domain.billing;

import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Share of an invoiced or requested amount owed by each customer of the folder. */
public record Distribution(Map<CustomerId, Money> shares) {

    public static final Distribution NONE = new Distribution(Map.of());

    public Distribution {
        Objects.requireNonNull(shares, "shares");
        shares = Collections.unmodifiableMap(new LinkedHashMap<>(shares));
    }

    public Money shareOf(CustomerId customerId) {
        return shares.getOrDefault(customerId, Money.ZERO);
    }

    public Money total() {
        return Money.sum(shares.values());
    }

    /** Invoice shares are rounded to 5 centimes, as on the legacy invoice form. */
    public Distribution roundedTo5Centimes() {
        Map<CustomerId, Money> rounded = new LinkedHashMap<>();
        shares.forEach((customer, share) -> rounded.put(customer, share.roundTo5Centimes()));
        return new Distribution(rounded);
    }
}
