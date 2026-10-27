package io.lexora.infrastructure.persistence.template;

import io.lexora.domain.template.Wording;
import io.lexora.domain.template.WordingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class WordingRepositoryAdapter implements WordingRepository {

    private final WordingJpaRepository jpa;

    WordingRepositoryAdapter(WordingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Wording> findAll() {
        return jpa.findAllByOrderByTextAsc().stream().map(e -> new Wording(e.id.longValue(), e.text)).toList();
    }

    @Override
    public Wording add(String text) {
        WordingEntity e = new WordingEntity();
        e.text = text;
        e = jpa.saveAndFlush(e);
        return new Wording(e.id.longValue(), e.text);
    }

    @Override
    public void delete(long id) {
        jpa.deleteById(toInt(id));
    }
}
