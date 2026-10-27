package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.util.Objects;

/**
 * Money paid out by the office ({@code book_payments}): bank payments, expenses and standing order executions.
 * A payment without folder belongs to the office books; with a folder, to the books of a tutelage folder.
 * Payments produced by a standing order are read-only: change the order, or delete the payment.
 */
public final class OutgoingPayment {

    private final OutgoingPaymentId id;
    private final FolderId folderId;
    private OutgoingPaymentDetails details;
    private Money tax;

    private OutgoingPayment(OutgoingPaymentId id, FolderId folderId, OutgoingPaymentDetails details, Money tax) {
        this.id = id;
        this.folderId = folderId;
        this.details = Objects.requireNonNull(details, "details");
        this.tax = tax;
    }

    public static OutgoingPayment record(FolderId folderId, OutgoingPaymentDetails details) {
        return new OutgoingPayment(null, folderId, details, includedTax(details));
    }

    public static OutgoingPayment restore(OutgoingPaymentId id, FolderId folderId, OutgoingPaymentDetails details,
                                          Money tax) {
        return new OutgoingPayment(Objects.requireNonNull(id, "id"), folderId, details, tax);
    }

    /** A manual entry (bank payment or expense); permanent payments only come from {@link StandingOrder}. */
    public static OutgoingPayment recordManual(FolderId folderId, OutgoingPaymentDetails details) {
        requireManualKind(details);
        return record(folderId, details);
    }

    public void update(OutgoingPaymentDetails details) {
        Objects.requireNonNull(details, "details");
        if (isPermanent()) {
            throw new DomainException(ErrorCode.PAYMENT_FROM_STANDING_ORDER, "Payment " + id.value()
                    + " was produced by a standing order and cannot be modified; change the order or delete the payment")
                            .with("paymentId", id.value());
        }
        requireManualKind(details);
        this.details = details;
        this.tax = includedTax(details);
    }

    /** The amount includes the VAT: tax = amount * rate / (1 + rate). */
    private static Money includedTax(OutgoingPaymentDetails details) {
        return details.taxRate().includedIn(details.amount());
    }

    private static void requireManualKind(OutgoingPaymentDetails details) {
        if (details.kind() == PaymentKind.PERMANENT) {
            throw new DomainException(ErrorCode.PERMANENT_KIND_RESERVED, "Only a standing order execution can produce a permanent payment");
        }
    }

    /** True when produced by a standing order. */
    public boolean isPermanent() {
        return details.kind() == PaymentKind.PERMANENT;
    }

    public boolean isPaid() {
        return details.paidOn() != null;
    }

    public OutgoingPaymentId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public OutgoingPaymentDetails details() {
        return details;
    }

    public Money amount() {
        return details.amount();
    }

    public Money tax() {
        return tax;
    }
}
