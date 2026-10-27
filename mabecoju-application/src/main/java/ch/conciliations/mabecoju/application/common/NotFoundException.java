package ch.conciliations.mabecoju.application.common;

/** Raised when a use case refers to something that does not exist (error code {@code NOT_FOUND}). */
public class NotFoundException extends RuntimeException {

    private final String what;
    private final Object id;

    public NotFoundException(String what, Object id) {
        super(what + " " + id + " not found");
        this.what = what;
        this.id = id;
    }

    /** Kind of thing looked for, in English (Invoice, Customer…). */
    public String what() {
        return what;
    }

    public Object id() {
        return id;
    }
}
