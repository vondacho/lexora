package io.lexora.domain.folder;

import java.util.Arrays;

/** Kind of mandate a folder represents (column {@code fold_type}). */
public enum FolderType {
    /** Legacy folders created before the type became mandatory. */
    UNDEFINED(0),
    INVOICING(1),
    TUTELAGE(2);

    private final int code;

    FolderType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static FolderType fromCode(Integer code) {
        if (code == null) {
            return UNDEFINED;
        }
        return Arrays.stream(values()).filter(t -> t.code == code).findFirst().orElse(UNDEFINED);
    }
}
