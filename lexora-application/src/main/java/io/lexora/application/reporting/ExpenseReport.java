package io.lexora.application.reporting;

import io.lexora.domain.booking.OutgoingPayment;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;

import java.util.List;

/** Office payments made over a period (legacy "relevé des sorties"). */
public record ExpenseReport(Period period, List<OutgoingPayment> payments) {

    public Money total() {
        return payments.stream().collect(Money.summing(OutgoingPayment::amount));
    }

    public Money taxTotal() {
        return payments.stream().collect(Money.summing(OutgoingPayment::tax));
    }
}
