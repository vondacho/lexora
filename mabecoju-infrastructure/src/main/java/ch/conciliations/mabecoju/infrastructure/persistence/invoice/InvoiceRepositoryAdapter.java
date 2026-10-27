package ch.conciliations.mabecoju.infrastructure.persistence.invoice;

import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.billing.Recall;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.Invoice;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.invoice.InvoicePayment;
import ch.conciliations.mabecoju.domain.invoice.InvoicePaymentRecord;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.invoice.InvoiceTerms;
import ch.conciliations.mabecoju.domain.invoice.PaymentId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.domain.ticket.TicketId;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.isTrue;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.rate;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.reference;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toInt;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toIntOrNull;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toIntOrZero;

@Repository
class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository jpa;
    private final InvoicePaymentJpaRepository payments;

    InvoiceRepositoryAdapter(InvoiceJpaRepository jpa, InvoicePaymentJpaRepository payments) {
        this.jpa = jpa;
        this.payments = payments;
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return jpa.findById(toInt(id.value())).map(InvoiceRepositoryAdapter::toDomain);
    }

    @Override
    public List<Invoice> findByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdOrderByDateDescIdDesc(toInt(folderId.value())));
    }

    @Override
    public List<Invoice> findOpen() {
        return toDomain(jpa.findByPaidOnIsNullOrderByDateAscIdAsc());
    }

    @Override
    public List<Invoice> findPaid() {
        return toDomain(jpa.findByPaidOnIsNotNullOrderByPaidOnDesc());
    }

    @Override
    public List<InvoicePaymentRecord> findPaymentsBetween(Period period) {
        return toRecords(payments.findWithFolderBetween(period.from(), period.to()));
    }

    @Override
    public List<InvoicePaymentRecord> findPaymentsByTicket(TicketId ticketId) {
        return toRecords(payments.findWithFolderByTicket(toInt(ticketId.value())));
    }

    @Override
    public List<InvoicePaymentRecord> findAllPayments() {
        return toRecords(payments.findAllWithFolder());
    }

    @Override
    public Optional<InvoiceId> findInvoiceOfPayment(PaymentId paymentId) {
        return payments.findInvoiceIdOf(toInt(paymentId.value())).map(InvoiceId::of);
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity e = invoice.id() == null
                ? new InvoiceEntity()
                : jpa.findById(toInt(invoice.id().value())).orElseThrow();
        InvoiceTerms terms = invoice.terms();
        e.folderId = toInt(invoice.folderId().value());
        e.date = terms.date();
        e.delay = terms.delayDays();
        e.comment = terms.comment();
        e.honorariesSum = toFloat(invoice.honorariesSum());
        e.advancedSum = toFloat(invoice.advancedSum());
        e.discountRate = toFloat(terms.discountRate());
        e.discount = toFloat(terms.discount());
        e.taxRate = toFloat(terms.taxRate());
        e.tax = toFloat(invoice.tax());
        e.amount = toFloat(invoice.amount());
        e.paidOn = invoice.paidOn();

        e.distribution.clear();
        invoice.distribution().shares().forEach((customer, share) ->
                e.distribution.add(new InvoiceDistributionEntity(e, toInt(customer.value()), toFloat(share))));

        Set<Integer> keptRecalls = invoice.recalls().stream().map(Recall::id).filter(Objects::nonNull)
                .map(id -> toInt(id.value())).collect(Collectors.toSet());
        e.recalls.removeIf(r -> !keptRecalls.contains(r.id));
        invoice.recalls().stream().filter(r -> r.id() == null).forEach(r -> {
            InvoiceRecallEntity recall = new InvoiceRecallEntity();
            recall.invoice = e;
            recall.date = r.date();
            recall.degree = r.degree();
            recall.amount = toFloat(r.amount());
            e.recalls.add(recall);
        });

        Set<Integer> keptPayments = invoice.payments().stream().map(InvoicePayment::id).filter(Objects::nonNull)
                .map(id -> toInt(id.value())).collect(Collectors.toSet());
        e.payments.removeIf(p -> !keptPayments.contains(p.id));
        invoice.payments().stream().filter(p -> p.id() == null).forEach(p -> {
            InvoicePaymentEntity payment = new InvoicePaymentEntity();
            payment.invoice = e;
            payment.customerId = toIntOrZero(p.customerId() == null ? null : p.customerId().value());
            payment.date = p.date();
            payment.amount = toFloat(p.amount());
            payment.ticketId = toIntOrNull(p.means().ticketId() == null ? null : p.means().ticketId().value());
            payment.iban = p.means().iban();
            payment.cash = p.means().cash();
            e.payments.add(payment);
        });

        InvoiceEntity saved = jpa.saveAndFlush(e);
        saved.payments.forEach(p -> p.invoiceId = saved.id);
        return toDomain(saved);
    }

    @Override
    public void delete(InvoiceId id) {
        jpa.deleteById(toInt(id.value()));
    }

    private static List<Invoice> toDomain(List<InvoiceEntity> entities) {
        return entities.stream().map(InvoiceRepositoryAdapter::toDomain).toList();
    }

    static Invoice toDomain(InvoiceEntity e) {
        Map<CustomerId, Money> shares = new LinkedHashMap<>();
        e.distribution.forEach(d -> shares.put(CustomerId.of(d.customerId), money(d.amount)));
        InvoiceTerms terms = new InvoiceTerms(e.date, e.delay == null ? 0 : e.delay, rate(e.discountRate),
                money(e.discount), rate(e.taxRate), new Distribution(shares), e.comment);
        List<Recall> recalls = e.recalls.stream()
                .map(r -> new Recall(RecallId.of(r.id), r.date, r.degree == null ? 0 : r.degree, money(r.amount)))
                .toList();
        List<InvoicePayment> paymentList = e.payments.stream().map(InvoiceRepositoryAdapter::toDomain).toList();
        return Invoice.restore(InvoiceId.of(e.id), FolderId.of(e.folderId), money(e.honorariesSum),
                money(e.advancedSum), terms, money(e.tax), money(e.amount), e.paidOn, paymentList, recalls);
    }

    static InvoicePayment toDomain(InvoicePaymentEntity p) {
        Long customer = reference(p.customerId);
        Long ticket = reference(p.ticketId);
        return new InvoicePayment(PaymentId.of(p.id), customer == null ? null : CustomerId.of(customer), p.date,
                money(p.amount), new PaymentMeans(ticket == null ? null : TicketId.of(ticket), p.iban, isTrue(p.cash)));
    }

    private static List<InvoicePaymentRecord> toRecords(List<Object[]> rows) {
        return rows.stream().map(row -> {
            InvoicePaymentEntity payment = (InvoicePaymentEntity) row[0];
            Integer folderId = (Integer) row[1];
            return new InvoicePaymentRecord(InvoiceId.of(payment.invoiceId), FolderId.of(folderId), toDomain(payment));
        }).toList();
    }
}
