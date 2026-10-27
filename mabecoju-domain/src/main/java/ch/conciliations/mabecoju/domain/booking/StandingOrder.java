package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/** A recurring monthly payment ("ordre permanent", {@code book_orders}). */
public final class StandingOrder {

    private final StandingOrderId id;
    private final FolderId folderId;
    private StandingOrderDetails details;
    private Money tax;

    private StandingOrder(StandingOrderId id, FolderId folderId, StandingOrderDetails details, Money tax) {
        this.id = id;
        this.folderId = folderId;
        this.details = Objects.requireNonNull(details, "details");
        this.tax = tax;
    }

    public static StandingOrder create(FolderId folderId, StandingOrderDetails details) {
        return new StandingOrder(null, folderId, details, details.taxRate().includedIn(details.amount()));
    }

    public static StandingOrder restore(StandingOrderId id, FolderId folderId, StandingOrderDetails details, Money tax) {
        return new StandingOrder(Objects.requireNonNull(id, "id"), folderId, details, tax);
    }

    public void update(StandingOrderDetails details) {
        this.details = Objects.requireNonNull(details, "details");
        this.tax = details.taxRate().includedIn(details.amount());
    }

    /**
     * Produces the payment of the given month, already paid on its execution date. Disabled orders produce nothing.
     */
    public Optional<OutgoingPayment> executeFor(YearMonth month) {
        if (!details.enabled()) {
            return Optional.empty();
        }
        LocalDate executionDate = month.atDay(Math.min(details.dayOfMonth(), month.lengthOfMonth()));
        OutgoingPaymentDetails payment = new OutgoingPaymentDetails(PaymentKind.PERMANENT, executionDate, null,
                details.creditor(), details.title(), null, details.amount(), details.taxRate(), executionDate);
        return Optional.of(OutgoingPayment.record(folderId, payment));
    }

    /** Month to execute next: the one after the last execution, or the current month when never executed. */
    public static YearMonth nextExecutionMonth(Optional<LocalDate> lastExecution, YearMonth currentMonth) {
        return lastExecution.map(date -> YearMonth.from(date).plusMonths(1)).orElse(currentMonth);
    }

    public boolean isEnabled() {
        return details.enabled();
    }

    public StandingOrderId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public StandingOrderDetails details() {
        return details;
    }

    public Money tax() {
        return tax;
    }
}
