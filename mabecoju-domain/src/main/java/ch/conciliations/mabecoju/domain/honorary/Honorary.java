package ch.conciliations.mabecoju.domain.honorary;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A billable service ("honoraire") recorded in a folder. It stays open until an invoice includes it.
 */
public final class Honorary {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final HonoraryId id;
    private final FolderId folderId;
    private HonoraryWork work;
    private Money amount;
    private InvoiceId invoiceId;

    private Honorary(HonoraryId id, FolderId folderId, HonoraryWork work, Money amount, InvoiceId invoiceId) {
        this.id = id;
        this.folderId = Objects.requireNonNull(folderId, "folderId");
        this.work = Objects.requireNonNull(work, "work");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.invoiceId = invoiceId;
    }

    public static Honorary record(FolderId folderId, HonoraryWork work) {
        return new Honorary(null, folderId, work, amountOf(work), null);
    }

    /** Restores a stored honorary keeping its stored amount, which may differ from today's formula. */
    public static Honorary restore(HonoraryId id, FolderId folderId, HonoraryWork work, Money amount, InvoiceId invoiceId) {
        return new Honorary(Objects.requireNonNull(id, "id"), folderId, work, amount, invoiceId);
    }

    /** Hourly rate times time spent, plus expenses. */
    public static Money amountOf(HonoraryWork work) {
        BigDecimal fee = work.hourlyRate().amount()
                .multiply(BigDecimal.valueOf(work.minutes()))
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
        Money expenses = work.expenses().stream().collect(Money.summing(Expense::amount));
        return Money.of(fee).plus(expenses);
    }

    /** Only an open honorary can be changed: an invoiced one is part of the invoice totals. */
    public void revise(HonoraryWork work) {
        requireOpen();
        this.work = Objects.requireNonNull(work, "work");
        this.amount = amountOf(work);
    }

    public void includeIn(InvoiceId invoiceId) {
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
    }

    /** Makes the honorary billable again, e.g. when its invoice is deleted. */
    public void release() {
        this.invoiceId = null;
    }

    /** Fails when the honorary is invoiced; delete or revise the invoice first. */
    public void requireOpen() {
        if (isInvoiced()) {
            throw new DomainException(ErrorCode.HONORARY_INVOICED, "Honorary " + (id == null ? "(new)" : id.value()) + " is invoiced on invoice "
                    + invoiceId.value() + "; delete the invoice first to change it")
                            .with("honoraryId", id == null ? null : id.value())
                            .with("invoiceId", invoiceId.value());
        }
    }

    public boolean isInvoiced() {
        return invoiceId != null;
    }

    public HonoraryId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public HonoraryWork work() {
        return work;
    }

    public Money amount() {
        return amount;
    }

    public InvoiceId invoiceId() {
        return invoiceId;
    }
}
