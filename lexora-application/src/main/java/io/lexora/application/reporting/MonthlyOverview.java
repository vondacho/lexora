package io.lexora.application.reporting;

import io.lexora.domain.shared.Money;

import java.time.Month;
import java.util.List;

/** Incomes and expenses per month, most recent year first (legacy "aperçu"). */
public record MonthlyOverview(List<Year> years) {

    public record Year(int year, List<MonthFigures> months) {

        public Money incomes() {
            return months.stream().collect(Money.summing(MonthFigures::incomes));
        }

        public Money expenses() {
            return months.stream().collect(Money.summing(MonthFigures::expenses));
        }
    }

    /**
     * @param advances        advances received
     * @param invoicePayments invoice payments received
     * @param expenses        office payments paid
     */
    public record MonthFigures(Month month, Money advances, Money invoicePayments, Money expenses) {

        public Money incomes() {
            return advances.plus(invoicePayments);
        }

        public Money balance() {
            return incomes().minus(expenses);
        }
    }
}
