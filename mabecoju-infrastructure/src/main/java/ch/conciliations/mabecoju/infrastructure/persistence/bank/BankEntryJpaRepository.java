package ch.conciliations.mabecoju.infrastructure.persistence.bank;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankEntryJpaRepository extends JpaRepository<BankEntryEntity, Integer> {

    boolean existsByReference(String reference);
}
