package ch.conciliations.mabecoju.infrastructure.persistence.booking;

import ch.conciliations.mabecoju.domain.booking.OutgoingPayment;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentDetails;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentId;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentRepository;
import ch.conciliations.mabecoju.domain.booking.PaymentKind;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Period;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.rate;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.reference;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toInt;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toIntOrNull;

@Repository
class OutgoingPaymentRepositoryAdapter implements OutgoingPaymentRepository {

    private final OutgoingPaymentJpaRepository jpa;

    OutgoingPaymentRepositoryAdapter(OutgoingPaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<OutgoingPayment> findById(OutgoingPaymentId id) {
        return jpa.findById(toInt(id.value())).map(OutgoingPaymentRepositoryAdapter::toDomain);
    }

    @Override
    public List<OutgoingPayment> findOfficePayments() {
        return toDomain(jpa.findByFolderIdIsNullOrderByIdDesc());
    }

    @Override
    public List<OutgoingPayment> findByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdOrderByPaidOnDescIdDesc(toInt(folderId.value())));
    }

    @Override
    public List<OutgoingPayment> findOfficePaidBetween(Period period) {
        return toDomain(jpa.findByFolderIdIsNullAndPaidOnBetweenOrderByPaidOnAscKindAsc(period.from(), period.to()));
    }

    @Override
    public List<OutgoingPayment> findOfficePaid() {
        return toDomain(jpa.findByFolderIdIsNullAndPaidOnIsNotNull());
    }

    @Override
    public Optional<LocalDate> lastPermanentPaymentDate(FolderId folderIdOrNull) {
        return jpa.findLastDate(PaymentKind.PERMANENT.code(),
                folderIdOrNull == null ? null : toInt(folderIdOrNull.value()));
    }

    @Override
    public OutgoingPayment save(OutgoingPayment payment) {
        OutgoingPaymentEntity e = payment.id() == null
                ? new OutgoingPaymentEntity()
                : jpa.findById(toInt(payment.id().value())).orElseThrow();
        OutgoingPaymentDetails d = payment.details();
        e.folderId = toIntOrNull(payment.folderId() == null ? null : payment.folderId().value());
        e.kind = d.kind().code();
        e.date = d.date();
        e.number = d.number();
        e.creditor = d.creditor();
        e.title = d.title();
        e.ticketNumber = d.ticketNumber();
        e.amount = toFloat(d.amount());
        e.taxRate = toFloat(d.taxRate());
        e.tax = toFloat(payment.tax());
        e.paidOn = d.paidOn();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(OutgoingPaymentId id) {
        jpa.deleteById(toInt(id.value()));
    }

    private static List<OutgoingPayment> toDomain(List<OutgoingPaymentEntity> entities) {
        return entities.stream().map(OutgoingPaymentRepositoryAdapter::toDomain).toList();
    }

    static OutgoingPayment toDomain(OutgoingPaymentEntity e) {
        Long folder = reference(e.folderId);
        return OutgoingPayment.restore(OutgoingPaymentId.of(e.id), folder == null ? null : FolderId.of(folder),
                new OutgoingPaymentDetails(PaymentKind.fromCode(e.kind), e.date, e.number, e.creditor, e.title,
                        e.ticketNumber, money(e.amount), rate(e.taxRate), e.paidOn),
                money(e.tax));
    }
}
