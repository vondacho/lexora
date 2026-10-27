package io.lexora.infrastructure.persistence.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OutgoingPaymentJpaRepository extends JpaRepository<OutgoingPaymentEntity, Integer> {

    List<OutgoingPaymentEntity> findByFolderIdIsNullOrderByIdDesc();

    List<OutgoingPaymentEntity> findByFolderIdOrderByPaidOnDescIdDesc(Integer folderId);

    List<OutgoingPaymentEntity> findByFolderIdIsNullAndPaidOnBetweenOrderByPaidOnAscKindAsc(LocalDate from, LocalDate to);

    List<OutgoingPaymentEntity> findByFolderIdIsNullAndPaidOnIsNotNull();

    @Query("""
            select max(p.date) from OutgoingPaymentEntity p
            where p.kind = :kind and ((:folderId is null and p.folderId is null) or p.folderId = :folderId)""")
    Optional<LocalDate> findLastDate(@Param("kind") Integer kind, @Param("folderId") Integer folderId);
}
