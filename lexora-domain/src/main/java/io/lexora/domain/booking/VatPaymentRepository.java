package io.lexora.domain.booking;

import java.util.List;
import java.util.Optional;

public interface VatPaymentRepository {

    Optional<VatPayment> findById(VatPaymentId id);

    /** All payments, most recent period first. */
    List<VatPayment> findAll();

    /** The VAT payment an office expense was booked for. */
    Optional<VatPayment> findByExpense(OutgoingPaymentId expenseId);

    VatPayment save(VatPayment payment);

    void delete(VatPaymentId id);
}
