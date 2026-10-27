package ch.conciliations.mabecoju.infrastructure.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordingJpaRepository extends JpaRepository<WordingEntity, Integer> {

    List<WordingEntity> findAllByOrderByTextAsc();
}
