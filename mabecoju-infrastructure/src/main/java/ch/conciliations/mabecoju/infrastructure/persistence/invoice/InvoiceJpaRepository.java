package ch.conciliations.mabecoju.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, Integer> {

    List<InvoiceEntity> findByFolderIdOrderByDateDescIdDesc(Integer folderId);

    List<InvoiceEntity> findByPaidOnIsNullOrderByDateAscIdAsc();

    List<InvoiceEntity> findByPaidOnIsNotNullOrderByPaidOnDesc();
}
