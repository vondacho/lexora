package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.shared.EntityId;

public record InvoiceId(long value) implements EntityId {

    public static InvoiceId of(long value) {
        return new InvoiceId(value);
    }
}
