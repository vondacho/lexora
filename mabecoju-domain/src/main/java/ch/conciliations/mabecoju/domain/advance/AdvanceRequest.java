package ch.conciliations.mabecoju.domain.advance;

import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.billing.Recall;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.billing.Settlement;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A request for a provision ("demande de provision"). It is paid through {@link Advance}s that reference it;
 * as advances are separate aggregates, the operations depending on them receive them as arguments.
 */
public final class AdvanceRequest {

    private final AdvanceRequestId id;
    private final FolderId folderId;
    private final Money honorariesSum;
    private final Money advancedSum;
    private final Integer legacyModelCode;
    private AdvanceRequestTerms terms;
    private LocalDate paidOn;
    private final List<Recall> recalls;

    private AdvanceRequest(AdvanceRequestId id, FolderId folderId, Money honorariesSum, Money advancedSum,
                           Integer legacyModelCode, AdvanceRequestTerms terms, LocalDate paidOn, List<Recall> recalls) {
        this.id = id;
        this.folderId = Objects.requireNonNull(folderId, "folderId");
        this.honorariesSum = Objects.requireNonNull(honorariesSum, "honorariesSum");
        this.advancedSum = Objects.requireNonNull(advancedSum, "advancedSum");
        this.legacyModelCode = legacyModelCode;
        this.terms = Objects.requireNonNull(terms, "terms");
        this.paidOn = paidOn;
        this.recalls = new ArrayList<>(recalls);
    }

    /**
     * @param honorariesSum open honoraries not covered by available advances, at request time
     * @param advancedSum   advances available at request time
     */
    public static AdvanceRequest create(FolderId folderId, Money honorariesSum, Money advancedSum,
                                        AdvanceRequestTerms terms) {
        return new AdvanceRequest(null, folderId, honorariesSum, advancedSum, null, terms, null, List.of());
    }

    public static AdvanceRequest restore(AdvanceRequestId id, FolderId folderId, Money honorariesSum,
                                         Money advancedSum, Integer legacyModelCode, AdvanceRequestTerms terms,
                                         LocalDate paidOn, List<Recall> recalls) {
        return new AdvanceRequest(Objects.requireNonNull(id, "id"), folderId, honorariesSum, advancedSum,
                legacyModelCode, terms, paidOn, recalls);
    }

    public void revise(AdvanceRequestTerms terms) {
        this.terms = Objects.requireNonNull(terms, "terms");
    }

    // ----- payment through advances ----------------------------------------------------------------------------

    public Money paidSum(Collection<Advance> advances) {
        return advances.stream().filter(a -> a.answers(id)).collect(Money.summing(Advance::amount));
    }

    public Money paidSumBy(CustomerId customerId, Collection<Advance> advances) {
        return advances.stream()
                .filter(a -> a.answers(id) && customerId.equals(a.customerId()))
                .collect(Money.summing(Advance::amount));
    }

    public Money remainingSum(Collection<Advance> advances) {
        return amount().minus(paidSum(advances));
    }

    public Money remainingSumFor(CustomerId customerId, Collection<Advance> advances) {
        return shareOf(customerId).minus(paidSumBy(customerId, advances));
    }

    public Money shareOf(CustomerId customerId) {
        return terms.distribution().shareOf(customerId);
    }

    /**
     * Settles the request, on the date of its last advance, once its advances cover the requested amount within
     * the tolerance.
     */
    public void settleIfCovered(Collection<Advance> advances) {
        List<Advance> own = advances.stream().filter(a -> a.answers(id)).toList();
        if (!isPaid() && isCovered(own)) {
            paidOn = own.stream().map(Advance::date).max(Comparator.naturalOrder()).orElseThrow();
        }
    }

    /** Reopens a paid request when its remaining advances no longer cover it (e.g. after an advance is cancelled). */
    public void reopenIfUncovered(Collection<Advance> advances) {
        List<Advance> own = advances.stream().filter(a -> a.answers(id)).toList();
        if (isPaid() && !isCovered(own)) {
            paidOn = null;
        }
    }

    private boolean isCovered(List<Advance> own) {
        return !own.isEmpty() && Settlement.isCovered(amount(), paidSum(own));
    }

    /** A request is invoiced once all its advances were deducted on an invoice. */
    public boolean isInvoiced(Collection<Advance> advances) {
        List<Advance> own = advances.stream().filter(a -> a.answers(id)).toList();
        return !own.isEmpty() && own.stream().allMatch(Advance::isInvoiced);
    }

    public void settleOn(LocalDate date) {
        this.paidOn = Objects.requireNonNull(date, "date");
    }

    public void reopen() {
        this.paidOn = null;
    }

    // ----- reminders -------------------------------------------------------------------------------------------

    public Recall recall(LocalDate date, Collection<Advance> advances) {
        Objects.requireNonNull(date, "date");
        if (isPaid()) {
            throw new DomainException(ErrorCode.DOCUMENT_PAID, "Advance request " + id.value() + " is paid and cannot be recalled")
                    .with("document", "P-" + id.value());
        }
        Recall recall = new Recall(null, date, recalls.size() + 1, remainingSum(advances));
        recalls.add(recall);
        return recall;
    }

    public void cancelRecall(RecallId recallId) {
        boolean removed = recalls.removeIf(r -> recallId.equals(r.id()));
        if (!removed) {
            throw new DomainException(ErrorCode.RECALL_NOT_OF_DOCUMENT, "Recall " + recallId.value() + " does not belong to advance request " + id.value())
                    .with("recallId", recallId.value())
                    .with("document", "P-" + id.value());
        }
    }

    public Optional<Recall> lastRecall() {
        return recalls.stream().max(Comparator.comparing(Recall::date).thenComparing(Recall::degree));
    }

    public LocalDate dueDate() {
        return terms.date().plusDays(terms.delayDays());
    }

    public long daysOverdue(LocalDate today) {
        return ChronoUnit.DAYS.between(dueDate(), today);
    }

    public boolean isPaid() {
        return paidOn != null;
    }

    public AdvanceRequestId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public AdvanceRequestTerms terms() {
        return terms;
    }

    public LocalDate date() {
        return terms.date();
    }

    public Money amount() {
        return terms.amount();
    }

    public Distribution distribution() {
        return terms.distribution();
    }

    public Money honorariesSum() {
        return honorariesSum;
    }

    public Money advancedSum() {
        return advancedSum;
    }

    /** Value of the unused legacy column {@code req_model_id}, preserved as is. */
    public Integer legacyModelCode() {
        return legacyModelCode;
    }

    public LocalDate paidOn() {
        return paidOn;
    }

    public List<Recall> recalls() {
        return List.copyOf(recalls);
    }
}
