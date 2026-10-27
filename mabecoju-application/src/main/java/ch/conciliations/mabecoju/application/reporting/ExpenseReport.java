package ch.conciliations.mabecoju.application.reporting;

import ch.conciliations.mabecoju.domain.booking.OutgoingPayment;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;

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
