package io.lexora.infrastructure.persistence.advance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdvanceJpaRepository extends JpaRepository<AdvanceEntity, Integer> {

    List<AdvanceEntity> findByFolderIdOrderByDateDescIdDesc(Integer folderId);

    List<AdvanceEntity> findByFolderIdAndInvoiceIdIsNullOrderByDateDescIdDesc(Integer folderId);

    List<AdvanceEntity> findByRequestIdOrderByDateAscIdAsc(Integer requestId);

    List<AdvanceEntity> findByInvoiceIdOrderByIdAsc(Integer invoiceId);

    List<AdvanceEntity> findByTicketIdOrderByDateAscIdAsc(Integer ticketId);

    List<AdvanceEntity> findAllByOrderByDateAscIdAsc();

    @Query("""
            select a from AdvanceEntity a, FolderEntity f
            where a.folderId = f.id and f.type = :folderType and a.date between :from and :to
            order by a.date, a.id""")
    List<AdvanceEntity> findReceivedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                            @Param("folderType") Integer folderType);

    @Query("""
            select a.folderId, sum(a.amount) from AdvanceEntity a
            where a.invoiceId is null and a.amount > 0
            group by a.folderId""")
    List<Object[]> sumNotInvoicedByFolder();
}
