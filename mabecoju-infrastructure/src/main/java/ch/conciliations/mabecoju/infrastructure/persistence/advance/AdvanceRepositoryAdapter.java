package ch.conciliations.mabecoju.infrastructure.persistence.advance;

import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceId;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderType;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.domain.ticket.TicketId;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.isTrue;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.reference;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toInt;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toIntOrNull;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toIntOrZero;

@Repository
class AdvanceRepositoryAdapter implements AdvanceRepository {

    private final AdvanceJpaRepository jpa;

    AdvanceRepositoryAdapter(AdvanceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Advance> findById(AdvanceId id) {
        return jpa.findById(toInt(id.value())).map(AdvanceRepositoryAdapter::toDomain);
    }

    @Override
    public List<Advance> findByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdOrderByDateDescIdDesc(toInt(folderId.value())));
    }

    @Override
    public List<Advance> findNotInvoicedByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdAndInvoiceIdIsNullOrderByDateDescIdDesc(toInt(folderId.value())));
    }

    @Override
    public List<Advance> findByRequest(AdvanceRequestId requestId) {
        return toDomain(jpa.findByRequestIdOrderByDateAscIdAsc(toInt(requestId.value())));
    }

    @Override
    public List<Advance> findByInvoice(InvoiceId invoiceId) {
        return toDomain(jpa.findByInvoiceIdOrderByIdAsc(toInt(invoiceId.value())));
    }

    @Override
    public List<Advance> findByTicket(TicketId ticketId) {
        return toDomain(jpa.findByTicketIdOrderByDateAscIdAsc(toInt(ticketId.value())));
    }

    @Override
    public List<Advance> findReceivedBetween(Period period, FolderType folderType) {
        return toDomain(jpa.findReceivedBetween(period.from(), period.to(), folderType.code()));
    }

    @Override
    public List<Advance> findAll() {
        return toDomain(jpa.findAllByOrderByDateAscIdAsc());
    }

    @Override
    public Map<FolderId, Money> sumNotInvoicedByFolder() {
        Map<FolderId, Money> sums = new LinkedHashMap<>();
        for (Object[] row : jpa.sumNotInvoicedByFolder()) {
            if (row[0] != null) {
                sums.put(FolderId.of(((Number) row[0]).longValue()), money(((Number) row[1]).doubleValue()));
            }
        }
        return sums;
    }

    @Override
    public Advance save(Advance advance) {
        AdvanceEntity e = advance.id() == null
                ? new AdvanceEntity()
                : jpa.findById(toInt(advance.id().value())).orElseThrow();
        e.folderId = toInt(advance.folderId().value());
        e.requestId = toIntOrNull(advance.requestId() == null ? null : advance.requestId().value());
        e.customerId = toIntOrZero(advance.customerId() == null ? null : advance.customerId().value());
        e.date = advance.date();
        e.amount = toFloat(advance.amount());
        e.ticketId = toIntOrNull(advance.means().ticketId() == null ? null : advance.means().ticketId().value());
        e.iban = advance.means().iban();
        e.cash = advance.means().cash();
        e.invoiceId = toIntOrNull(advance.invoiceId() == null ? null : advance.invoiceId().value());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(AdvanceId id) {
        jpa.deleteById(toInt(id.value()));
    }

    private static List<Advance> toDomain(List<AdvanceEntity> entities) {
        return entities.stream().map(AdvanceRepositoryAdapter::toDomain).toList();
    }

    static Advance toDomain(AdvanceEntity e) {
        Long request = reference(e.requestId);
        Long customer = reference(e.customerId);
        Long ticket = reference(e.ticketId);
        Long invoice = reference(e.invoiceId);
        return Advance.restore(AdvanceId.of(e.id), FolderId.of(e.folderId == null ? 0 : e.folderId),
                request == null ? null : AdvanceRequestId.of(request),
                customer == null ? null : CustomerId.of(customer),
                e.date, money(e.amount),
                new PaymentMeans(ticket == null ? null : TicketId.of(ticket), e.iban, isTrue(e.cash)),
                invoice == null ? null : InvoiceId.of(invoice));
    }
}
