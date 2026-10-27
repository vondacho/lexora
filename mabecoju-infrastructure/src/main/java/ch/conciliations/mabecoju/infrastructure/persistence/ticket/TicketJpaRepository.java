package ch.conciliations.mabecoju.infrastructure.persistence.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Integer> {

    Optional<TicketEntity> findFirstByNumberOrderByIdAsc(String number);

    List<TicketEntity> findByFolderIdOrderByIdAsc(Integer folderId);

    List<TicketEntity> findByInvoiceIdOrderByIdAsc(Integer invoiceId);

    List<TicketEntity> findByRequestIdOrderByIdAsc(Integer requestId);
}
