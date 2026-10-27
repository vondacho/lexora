package io.lexora.infrastructure.web;

import io.lexora.domain.billing.Distribution;
import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.billing.Recall;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.shared.EntityId;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;
import io.lexora.domain.ticket.TicketId;
import io.lexora.infrastructure.web.api.model.DateInputDto;
import io.lexora.infrastructure.web.api.model.RecallDto;
import io.lexora.infrastructure.web.api.model.ShareDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Conversions between the generated API models and the domain types. Rates travel as percentages. */
public final class Json {

    private Json() {
    }

    public static BigDecimal amount(Money money) {
        return money == null ? null : money.amount();
    }

    public static Money money(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }

    public static BigDecimal percent(Rate rate) {
        return rate == null ? null : rate.percent();
    }

    public static Rate rate(BigDecimal percent) {
        return percent == null ? Rate.ZERO : Rate.ofPercent(percent);
    }

    public static Long id(EntityId id) {
        return id == null ? null : id.value();
    }

    public static int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    public static LocalDate dateOf(DateInputDto input) {
        return input == null ? null : input.getDate();
    }

    /** Converts between enums having the same constant names (domain and API). */
    public static <E extends Enum<E>> E sameName(Enum<?> value, Class<E> target) {
        return value == null ? null : Enum.valueOf(target, value.name());
    }

    public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
        return list == null ? List.of() : list.stream().map(mapper).toList();
    }

    public static Distribution distribution(List<ShareDto> shares) {
        Map<CustomerId, Money> map = new LinkedHashMap<>();
        if (shares != null) {
            shares.forEach(s -> map.put(CustomerId.of(s.getCustomerId()), Money.of(s.getAmount())));
        }
        return new Distribution(map);
    }

    public static RecallDto recall(Recall recall) {
        return new RecallDto().id(id(recall.id())).date(recall.date()).degree(recall.degree())
                .amount(amount(recall.amount()));
    }

    public static PaymentMeans means(Long ticketId, String iban, Boolean cash) {
        return new PaymentMeans(ticketId == null ? null : TicketId.of(ticketId), iban, isTrue(cash));
    }
}
