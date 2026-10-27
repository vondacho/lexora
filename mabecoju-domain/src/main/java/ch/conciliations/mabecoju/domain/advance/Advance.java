package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A provision ("avance") received from a customer, either in answer to an advance request or spontaneously.
 * Advances are deducted from the next invoice of the folder, which then consumes them.
 */
public final class Advance {

    private final AdvanceId id;
    private final FolderId folderId;
    private final AdvanceRequestId requestId;
    private final CustomerId customerId;
    private final LocalDate date;
    private final Money amount;
    private final PaymentMeans means;
    private InvoiceId invoiceId;

    private Advance(AdvanceId id, FolderId folderId, AdvanceRequestId requestId, CustomerId customerId,
                    LocalDate date, Money amount, PaymentMeans means, InvoiceId invoiceId) {
        this.id = id;
        this.folderId = Objects.requireNonNull(folderId, "folderId");
        this.requestId = requestId;
        this.customerId = customerId;
        this.date = date;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.means = means == null ? PaymentMeans.UNKNOWN : means;
        this.invoiceId = invoiceId;
    }

    public static Advance receive(FolderId folderId, AdvanceRequestId requestId, CustomerId customerId,
                                  LocalDate date, Money amount, PaymentMeans means) {
        Objects.requireNonNull(date, "date");
        if (amount == null || !amount.isPositive()) {
            throw new DomainException(ErrorCode.AMOUNT_NOT_POSITIVE, "An advance amount must be positive")
                    .with("field", "advance");
        }
        return new Advance(null, folderId, requestId, customerId, date, amount, means, null);
    }

    public static Advance restore(AdvanceId id, FolderId folderId, AdvanceRequestId requestId, CustomerId customerId,
                                  LocalDate date, Money amount, PaymentMeans means, InvoiceId invoiceId) {
        return new Advance(Objects.requireNonNull(id, "id"), folderId, requestId, customerId, date, amount, means,
                invoiceId);
    }

    public void deductOn(InvoiceId invoiceId) {
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
    }

    /** Makes the advance available again, e.g. when the invoice it was deducted on is deleted. */
    public void release() {
        this.invoiceId = null;
    }

    public boolean isInvoiced() {
        return invoiceId != null;
    }

    public boolean answers(AdvanceRequestId requestId) {
        return requestId != null && requestId.equals(this.requestId);
    }

    public AdvanceId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public AdvanceRequestId requestId() {
        return requestId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public LocalDate date() {
        return date;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMeans means() {
        return means;
    }

    public InvoiceId invoiceId() {
        return invoiceId;
    }
}
