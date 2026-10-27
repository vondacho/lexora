package io.lexora.domain.template;

import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @param content      HTML body with placeholders filled when printing
 * @param addressCount number of addresses printed on the document
 * @param layouts      one layout per address slot ({@value #SLOTS} slots)
 */
public record DocumentModelDetails(
        String name,
        DocumentType type,
        String title,
        String content,
        int addressCount,
        List<PageLayout> layouts) {

    public static final int SLOTS = 4;

    public DocumentModelDetails {
        Strings.requireNotBlank(name, "Model name");
        Strings.requireMaxLength(name, 80, "Model name");
        Objects.requireNonNull(type, "type");
        content = content == null ? "" : content;
        List<PageLayout> slots = new ArrayList<>(layouts == null ? List.of() : layouts);
        if (slots.size() > SLOTS) {
            throw new DomainException(ErrorCode.TOO_MANY_LAYOUTS, "A model has at most " + SLOTS + " layouts")
                    .with("max", SLOTS);
        }
        while (slots.size() < SLOTS) {
            slots.add(PageLayout.NONE);
        }
        layouts = List.copyOf(slots);
    }
}
