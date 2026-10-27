package io.lexora.domain.shared;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Raised when an operation would break a business rule. The {@link ErrorCode} and its parameters let clients show
 * the reason in their language; the message is English, for logs.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> params = new LinkedHashMap<>();

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    /** Adds a parameter of the code (see {@link ErrorCode}); null values are left out. */
    public DomainException with(String name, Object value) {
        if (value != null) {
            params.put(name, value instanceof Number || value instanceof Boolean ? value : String.valueOf(value));
        }
        return this;
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }
}
