package io.lexora.infrastructure.persistence.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VatPaymentJpaRepository extends JpaRepository<VatPaymentEntity, Integer> {

    List<VatPaymentEntity> findAllByOrderByFromDesc();

    Optional<VatPaymentEntity> findByExpenseId(Integer expenseId);
}
