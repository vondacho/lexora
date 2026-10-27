package io.lexora.domain.template;

import java.util.Arrays;

/** Kind of document a model is used for (column {@code mode_type}). */
public enum DocumentType {
    UNDEFINED(0),
    INVOICE(1),
    ADVANCE_REQUEST(2),
    RECALL(3);

    private final int code;

    DocumentType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DocumentType fromCode(Integer code) {
        if (code == null) {
            return UNDEFINED;
        }
        return Arrays.stream(values()).filter(t -> t.code == code).findFirst().orElse(UNDEFINED);
    }
}
