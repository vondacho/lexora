package io.lexora.domain.invoice;

import io.lexora.domain.billing.Distribution;
import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.billing.Recall;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    private static final FolderId FOLDER = FolderId.of(1);
    private static final CustomerId ALICE = CustomerId.of(10);
    private static final CustomerId BOB = CustomerId.of(11);
    private static final LocalDate DATE = LocalDate.of(2026, 3, 1);

    private static Invoice issue() {
        InvoiceTerms terms = new InvoiceTerms(DATE, 30, Rate.ZERO, Money.of("50.03"), Rate.of("0.077"),
                new Distribution(Map.of(ALICE, Money.of("361.52"), BOB, Money.of("361.53"))), null);
        return Invoice.issue(FOLDER, Money.of("1000.00"), Money.of("300.00"), terms);
    }

    @Test
    void computes_discount_tax_and_amount_like_the_legacy_invoice_form() {
        Invoice invoice = issue();

        assertThat(invoice.discount()).isEqualTo(Money.of("50.05"));   // rounded to 5 centimes
        assertThat(invoice.tax()).isEqualTo(Money.of("73.10"));        // 949.95 * 7.7 % truncated to 10 centimes
        assertThat(invoice.amount()).isEqualTo(Money.of("723.05"));    // 949.95 + 73.10 - 300
        assertThat(invoice.shareOf(ALICE)).isEqualTo(Money.of("361.50"));
        assertThat(invoice.dueDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void is_settled_on_the_last_payment_date_once_covered_within_one_franc() {
        Invoice invoice = issue();

        invoice.recordPayment(ALICE, LocalDate.of(2026, 3, 10), Money.of("400.00"), PaymentMeans.UNKNOWN);
        assertThat(invoice.isPaid()).isFalse();

        invoice.recordPayment(BOB, LocalDate.of(2026, 3, 20), Money.of("322.10"), PaymentMeans.UNKNOWN);
        assertThat(invoice.isPaid()).isTrue();
        assertThat(invoice.paidOn()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(invoice.remainingSum()).isEqualTo(Money.of("0.95"));
        assertThat(invoice.remainingSumFor(ALICE)).isEqualTo(Money.of("-38.50"));
    }

    @Test
    void reopens_when_a_payment_is_cancelled() {
        Invoice invoice = restoredWithPayments(List.of(
                new InvoicePayment(PaymentId.of(1), ALICE, LocalDate.of(2026, 3, 10), Money.of("723.05"), PaymentMeans.UNKNOWN)));
        assertThat(invoice.isPaid()).isTrue();

        invoice.cancelPayment(PaymentId.of(1));

        assertThat(invoice.isPaid()).isFalse();
        assertThat(invoice.payments()).isEmpty();
    }

    @Test
    void stays_settled_when_remaining_payments_still_cover_it() {
        Invoice invoice = restoredWithPayments(List.of(
                new InvoicePayment(PaymentId.of(1), ALICE, LocalDate.of(2026, 3, 10), Money.of("723.05"), PaymentMeans.UNKNOWN),
                new InvoicePayment(PaymentId.of(2), ALICE, LocalDate.of(2026, 3, 12), Money.of("5.00"), PaymentMeans.UNKNOWN)));

        invoice.cancelPayment(PaymentId.of(2));

        assertThat(invoice.paidOn()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void recalls_increase_in_degree_and_claim_the_remaining_amount() {
        Invoice invoice = issue();
        invoice.recordPayment(ALICE, LocalDate.of(2026, 3, 10), Money.of("23.05"), PaymentMeans.UNKNOWN);

        Recall first = invoice.recall(LocalDate.of(2026, 4, 15));
        Recall second = invoice.recall(LocalDate.of(2026, 5, 15));

        assertThat(first.degree()).isEqualTo(1);
        assertThat(second.degree()).isEqualTo(2);
        assertThat(second.amount()).isEqualTo(Money.of("700.00"));
        assertThat(invoice.lastRecall()).contains(second);
    }

    @Test
    void a_paid_invoice_cannot_be_recalled() {
        Invoice invoice = issue();
        invoice.settleOn(DATE);

        assertThatThrownBy(() -> invoice.recall(DATE)).isInstanceOf(DomainException.class);
    }

    @Test
    void rejects_non_positive_payments() {
        assertThatThrownBy(() -> issue().recordPayment(ALICE, DATE, Money.ZERO, null))
                .isInstanceOf(DomainException.class);
    }

    private static Invoice restoredWithPayments(List<InvoicePayment> payments) {
        Invoice issued = issue();
        LocalDate paidOn = payments.stream().map(InvoicePayment::date).max(LocalDate::compareTo).orElse(null);
        return Invoice.restore(InvoiceId.of(5), FOLDER, issued.honorariesSum(), issued.advancedSum(), issued.terms(),
                issued.tax(), issued.amount(), paidOn, payments, List.of());
    }
}
