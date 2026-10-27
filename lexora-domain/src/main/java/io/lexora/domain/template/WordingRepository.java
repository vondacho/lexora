package io.lexora.domain.template;

import java.util.List;

public interface WordingRepository {

    /** Wordings in alphabetical order. */
    List<Wording> findAll();

    Wording add(String text);

    void delete(long id);
}
