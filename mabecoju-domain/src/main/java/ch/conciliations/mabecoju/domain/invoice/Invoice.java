package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.billing.Recall;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.billing.Settlement;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Rate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A fee note ("mémoire d'honoraires") billing the open honoraries of a folder, net of the advances
 * already received. Payments and reminders are part of the invoice.
 *
 * <pre>
 * subtotal = honoraries - discount
 * tax      = subtotal * tax rate, truncated to 10 centimes
 * amount   = subtotal + tax - advances
 * </pre>
 */
public final class Invoice {

    private final InvoiceId id;
    private final FolderId folderId;
    private final Money honorariesSum;
    private final Money advancedSum;
    private InvoiceTerms terms;
    private Money tax;
    private Money amount;
    private LocalDate paidOn;
    private final List<InvoicePayment> payments;
    private final List<Recall> recalls;

    private Invoice(InvoiceId id, FolderId folderId, Money honorariesSum, Money advancedSum, InvoiceTerms terms,
                    Money tax, Money amount, LocalDate paidOn, List<InvoicePayment> payments, List<Recall> recalls) {
        this.id = id;
        this.folderId = Objects.requireNonNull(folderId, "folderId");
        this.honorariesSum = Objects.requireNonNull(honorariesSum, "honorariesSum");
        this.advancedSum = Objects.requireNonNull(advancedSum, "advancedSum");
        this.terms = Objects.requireNonNull(terms, "terms");
        this.tax = tax;
        this.amount = amount;
        this.paidOn = paidOn;
        this.payments = new ArrayList<>(payments);
        this.recalls = new ArrayList<>(recalls);
    }

    /**
     * Issues an invoice for the given sums of open honoraries and available advances.
     */
    public static Invoice issue(FolderId folderId, Money honorariesSum, Money advancedSum, InvoiceTerms terms) {
        Invoice invoice = new Invoice(null, folderId, honorariesSum, advancedSum, terms, Money.ZERO, Money.ZERO,
                null, List.of(), List.of());
        invoice.applyTerms(terms);
        return invoice;
    }

    public static Invoice restore(InvoiceId id, FolderId folderId, Money honorariesSum, Money advancedSum,
                                  InvoiceTerms terms, Money tax, Money amount, LocalDate paidOn,
                                  List<InvoicePayment> payments, List<Recall> recalls) {
        return new Invoice(Objects.requireNonNull(id, "id"), folderId, honorariesSum, advancedSum, terms, tax, amount,
                paidOn, payments, recalls);
    }

    /** Changes date, delay, discount, tax rate or distribution and recomputes the totals. */
    public void revise(InvoiceTerms terms) {
        applyTerms(terms);
    }

    private void applyTerms(InvoiceTerms requested) {
        Money discount = requested.discount().roundTo5Centimes();
        this.terms = new InvoiceTerms(requested.date(), requested.delayDays(), requested.discountRate(), discount,
                requested.taxRate(), requested.distribution().roundedTo5Centimes(), requested.comment());
        Money subtotal = honorariesSum.minus(discount);
        this.tax = Money.floorTo10Centimes(requested.taxRate().applyTo(subtotal));
        this.amount = subtotal.plus(tax).minus(advancedSum);
    }

    // ----- payments --------------------------------------------------------------------------------------------

    /**
     * Records a payment. The invoice is settled, at the date of its last payment, once the payments cover the
     * amount within the tolerance.
     */
    public void recordPayment(CustomerId customerId, LocalDate date, Money paid, PaymentMeans means) {
        Objects.requireNonNull(date, "date");
        if (paid == null || !paid.isPositive()) {
            throw new DomainException(ErrorCode.AMOUNT_NOT_POSITIVE, "A payment amount must be positive")
                    .with("field", "payment");
        }
        payments.add(new InvoicePayment(null, customerId, date, paid, means == null ? PaymentMeans.UNKNOWN : means));
        settleIfCovered();
    }

    /** Removes a payment; the invoice is reopened unless the remaining payments still cover it. */
    public InvoicePayment cancelPayment(PaymentId paymentId) {
        InvoicePayment payment = payments.stream()
                .filter(p -> paymentId.equals(p.id()))
                .findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_OF_INVOICE, "Payment " + paymentId.value() + " does not belong to invoice " + idValue())
                        .with("paymentId", paymentId.value())
                        .with("invoiceId", idValue()));
        payments.remove(payment);
        paidOn = null;
        settleIfCovered();
        return payment;
    }

    private void settleIfCovered() {
        if (!isPaid() && !payments.isEmpty() && Settlement.isCovered(amount, paidSum())) {
            paidOn = payments.stream().map(InvoicePayment::date).max(Comparator.naturalOrder()).orElseThrow();
        }
    }

    /** Marks the invoice as paid on the given date regardless of the payments ("solder"). */
    public void settleOn(LocalDate date) {
        this.paidOn = Objects.requireNonNull(date, "date");
    }

    public void reopen() {
        this.paidOn = null;
    }

    public Money paidSum() {
        return payments.stream().collect(Money.summing(InvoicePayment::amount));
    }

    public Money remainingSum() {
        return amount.minus(paidSum());
    }

    public Money paidSumBy(CustomerId customerId) {
        return payments.stream()
                .filter(p -> customerId.equals(p.customerId()))
                .collect(Money.summing(InvoicePayment::amount));
    }

    /** What a customer still owes: his share minus what he already paid. */
    public Money remainingSumFor(CustomerId customerId) {
        return shareOf(customerId).minus(paidSumBy(customerId));
    }

    public Money shareOf(CustomerId customerId) {
        return terms.distribution().shareOf(customerId);
    }

    // ----- reminders -------------------------------------------------------------------------------------------

    /** Issues the next reminder for the amount still due. */
    public Recall recall(LocalDate date) {
        Objects.requireNonNull(date, "date");
        if (isPaid()) {
            throw new DomainException(ErrorCode.DOCUMENT_PAID, "Invoice " + idValue() + " is paid and cannot be recalled")
                    .with("document", "F-" + idValue());
        }
        Recall recall = new Recall(null, date, recalls.size() + 1, remainingSum());
        recalls.add(recall);
        return recall;
    }

    public void cancelRecall(RecallId recallId) {
        boolean removed = recalls.removeIf(r -> recallId.equals(r.id()));
        if (!removed) {
            throw new DomainException(ErrorCode.RECALL_NOT_OF_DOCUMENT, "Recall " + recallId.value() + " does not belong to invoice " + idValue())
                    .with("recallId", recallId.value())
                    .with("document", "F-" + idValue());
        }
    }

    public Optional<Recall> lastRecall() {
        return recalls.stream().max(Comparator.comparing(Recall::date).thenComparing(Recall::degree));
    }

    // ----- terms -----------------------------------------------------------------------------------------------

    public LocalDate dueDate() {
        return terms.date().plusDays(terms.delayDays());
    }

    /** Days elapsed since the due date; negative while not yet due. */
    public long daysOverdue(LocalDate today) {
        return ChronoUnit.DAYS.between(dueDate(), today);
    }

    public boolean isPaid() {
        return paidOn != null;
    }

    private Object idValue() {
        return id == null ? "(new)" : id.value();
    }

    public InvoiceId id() {
        return id;
    }

    public FolderId folderId() {
        return folderId;
    }

    public InvoiceTerms terms() {
        return terms;
    }

    public LocalDate date() {
        return terms.date();
    }

    public Money honorariesSum() {
        return honorariesSum;
    }

    public Money advancedSum() {
        return advancedSum;
    }

    public Money discount() {
        return terms.discount();
    }

    public Rate taxRate() {
        return terms.taxRate();
    }

    public Money tax() {
        return tax;
    }

    public Money amount() {
        return amount;
    }

    public Distribution distribution() {
        return terms.distribution();
    }

    public LocalDate paidOn() {
        return paidOn;
    }

    public List<InvoicePayment> payments() {
        return List.copyOf(payments);
    }

    public List<Recall> recalls() {
        return List.copyOf(recalls);
    }
}
