package io.lexora.infrastructure.persistence.ticket;

import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.ticket.Ticket;
import io.lexora.domain.ticket.TicketId;
import io.lexora.domain.ticket.TicketRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.reference;
import static io.lexora.infrastructure.persistence.support.Conversions.toInt;
import static io.lexora.infrastructure.persistence.support.Conversions.toIntOrNull;

@Repository
class TicketRepositoryAdapter implements TicketRepository {

    private final TicketJpaRepository jpa;

    TicketRepositoryAdapter(TicketJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Ticket> findById(TicketId id) {
        return jpa.findById(toInt(id.value())).map(TicketRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Ticket> findByNumber(String number) {
        return jpa.findFirstByNumberOrderByIdAsc(number).map(TicketRepositoryAdapter::toDomain);
    }

    @Override
    public List<Ticket> findByFolder(FolderId folderId) {
        return jpa.findByFolderIdOrderByIdAsc(toInt(folderId.value())).stream()
                .map(TicketRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Ticket> findByInvoice(InvoiceId invoiceId) {
        return jpa.findByInvoiceIdOrderByIdAsc(toInt(invoiceId.value())).stream()
                .map(TicketRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Ticket> findByRequest(AdvanceRequestId requestId) {
        return jpa.findByRequestIdOrderByIdAsc(toInt(requestId.value())).stream()
                .map(TicketRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity e = ticket.id() == null
                ? new TicketEntity()
                : jpa.findById(toInt(ticket.id().value())).orElseThrow();
        e.number = ticket.number();
        e.folderId = toIntOrNull(ticket.folderId() == null ? null : ticket.folderId().value());
        e.customerId = toIntOrNull(ticket.customerId() == null ? null : ticket.customerId().value());
        e.invoiceId = toIntOrNull(ticket.invoiceId() == null ? null : ticket.invoiceId().value());
        e.requestId = toIntOrNull(ticket.requestId() == null ? null : ticket.requestId().value());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(TicketId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static Ticket toDomain(TicketEntity e) {
        Long folder = reference(e.folderId);
        Long customer = reference(e.customerId);
        Long invoice = reference(e.invoiceId);
        Long request = reference(e.requestId);
        return Ticket.restore(TicketId.of(e.id), e.number, folder == null ? null : FolderId.of(folder),
                customer == null ? null : CustomerId.of(customer), invoice == null ? null : InvoiceId.of(invoice),
                request == null ? null : AdvanceRequestId.of(request));
    }
}
