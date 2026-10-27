package io.lexora.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoicePaymentJpaRepository extends JpaRepository<InvoicePaymentEntity, Integer> {

    /** Rows of [payment, folder id]. */
    @Query("""
            select p, i.folderId from InvoicePaymentEntity p join p.invoice i
            where p.date between :from and :to
            order by p.date, p.id""")
    List<Object[]> findWithFolderBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select p, i.folderId from InvoicePaymentEntity p join p.invoice i
            where p.ticketId = :ticketId
            order by p.date, p.id""")
    List<Object[]> findWithFolderByTicket(@Param("ticketId") Integer ticketId);

    @Query("select p, i.folderId from InvoicePaymentEntity p join p.invoice i order by p.date, p.id")
    List<Object[]> findAllWithFolder();

    @Query("select p.invoiceId from InvoicePaymentEntity p where p.id = :id")
    Optional<Integer> findInvoiceIdOf(@Param("id") Integer id);
}
