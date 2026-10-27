package io.lexora.infrastructure.persistence.honorary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HonoraryJpaRepository extends JpaRepository<HonoraryEntity, Integer> {

    List<HonoraryEntity> findByFolderIdOrderByDateAscIdAsc(Integer folderId);

    List<HonoraryEntity> findByFolderIdAndInvoiceIdIsNullOrderByDateAscIdAsc(Integer folderId);

    List<HonoraryEntity> findByInvoiceIdOrderByDateAscIdAsc(Integer invoiceId);

    List<HonoraryEntity> findByDateBetweenOrderByDateAscIdAsc(LocalDate from, LocalDate to);

    @Query("""
            select h.folderId, sum(h.amount) from HonoraryEntity h
            where h.invoiceId is null and h.amount > 0
            group by h.folderId""")
    List<Object[]> sumNotInvoicedByFolder();
}
