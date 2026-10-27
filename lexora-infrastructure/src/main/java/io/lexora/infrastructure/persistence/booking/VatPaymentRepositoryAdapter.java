package io.lexora.infrastructure.persistence.booking;

import io.lexora.domain.booking.OutgoingPaymentId;
import io.lexora.domain.booking.VatPayment;
import io.lexora.domain.booking.VatPaymentId;
import io.lexora.domain.booking.VatPaymentRepository;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import io.lexora.domain.shared.Rate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class VatPaymentRepositoryAdapter implements VatPaymentRepository {

    private final VatPaymentJpaRepository jpa;

    VatPaymentRepositoryAdapter(VatPaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<VatPayment> findById(VatPaymentId id) {
        return jpa.findById(toInt(id.value())).map(VatPaymentRepositoryAdapter::toDomain);
    }

    @Override
    public List<VatPayment> findAll() {
        return jpa.findAllByOrderByFromDesc().stream().map(VatPaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<VatPayment> findByExpense(OutgoingPaymentId expenseId) {
        return jpa.findByExpenseId(toInt(expenseId.value())).map(VatPaymentRepositoryAdapter::toDomain);
    }

    @Override
    public VatPayment save(VatPayment payment) {
        VatPaymentEntity e = payment.id() == null
                ? new VatPaymentEntity()
                : jpa.findById(toInt(payment.id().value())).orElseThrow();
        e.from = payment.period().from();
        e.to = payment.period().to();
        e.revenue = payment.revenue().amount();
        e.rate = payment.rate().value();
        e.amount = payment.amount().amount();
        e.paidOn = payment.paidOn();
        e.expenseId = payment.expenseId() == null ? null : toInt(payment.expenseId().value());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(VatPaymentId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static VatPayment toDomain(VatPaymentEntity e) {
        return VatPayment.restore(VatPaymentId.of(e.id), Period.of(e.from, e.to), Money.of(e.revenue),
                Rate.of(e.rate), Money.of(e.amount), e.paidOn,
                e.expenseId == null ? null : OutgoingPaymentId.of(e.expenseId));
    }
}
