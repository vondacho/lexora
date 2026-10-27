package io.lexora.application.reporting;

import io.lexora.application.common.Ids;
import io.lexora.domain.booking.OutgoingPayment;
import io.lexora.domain.booking.OutgoingPaymentRepository;
import io.lexora.domain.booking.VatPayment;
import io.lexora.domain.booking.VatPaymentId;
import io.lexora.domain.booking.VatPaymentRepository;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

/** VAT paid to the tax administration, period by period (net tax rate method). */
@Transactional
public class VatUseCases {

    private final VatPaymentRepository payments;
    private final OutgoingPaymentRepository expenses;
    private final ReportingUseCases reports;

    public VatUseCases(VatPaymentRepository payments, OutgoingPaymentRepository expenses, ReportingUseCases reports) {
        this.payments = payments;
        this.expenses = expenses;
        this.reports = reports;
    }

    public List<VatPayment> listPayments() {
        return payments.findAll();
    }

    /**
     * Records that the VAT of a period was paid. The money received and the rate are taken from the income report of
     * the period at the time of recording, so later corrections of the books do not change what was paid.
     * The amount paid is booked as a paid office expense, linked to the VAT payment.
     *
     * @param amount what was paid, or null for the VAT due according to the books
     */
    public VatPayment recordPayment(Period period, Money amount, LocalDate paidOn) {
        payments.findAll().stream().filter(p -> p.overlaps(period)).findFirst().ifPresent(p -> {
            throw new DomainException(ErrorCode.VAT_PERIOD_OVERLAP, "The VAT from " + p.period().from() + " to " + p.period().to()
                    + " was already paid on " + p.paidOn() + "; periods cannot overlap")
                            .with("from", p.period().from())
                            .with("to", p.period().to())
                            .with("paidOn", p.paidOn());
        });
        IncomeReport incomes = reports.incomeReport(period);
        VatPayment payment = VatPayment.record(period, incomes.total(), incomes.vatRate(),
                amount == null ? incomes.vatDue() : amount, paidOn);
        OutgoingPayment expense = expenses.save(OutgoingPayment.recordManual(null, payment.expenseDetails()));
        payment.bookedAs(expense.id());
        return payments.save(payment);
    }

    /** Deletes the VAT payment and the office expense booked for it. */
    public void deletePayment(VatPaymentId id) {
        VatPayment payment = Ids.require(payments.findById(id), "VAT payment", id.value());
        payments.delete(id);
        if (payment.expenseId() != null && expenses.findById(payment.expenseId()).isPresent()) {
            expenses.delete(payment.expenseId());
        }
    }
}
