package ch.conciliations.mabecoju.domain.shared;

/** Small text helpers shared by the domain. */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String requireNotBlank(String value, String what) {
        if (isBlank(value)) {
            throw new DomainException(ErrorCode.FIELD_MANDATORY, what + " is mandatory").with("field", what);
        }
        return value.trim();
    }

    public static String requireMaxLength(String value, int maxLength, String what) {
        if (value != null && value.length() > maxLength) {
            throw new DomainException(ErrorCode.FIELD_TOO_LONG, what + " must not exceed " + maxLength + " characters")
                    .with("field", what)
                    .with("max", maxLength);
        }
        return value;
    }
}
