package io.lexora.domain.advance;

import io.lexora.domain.billing.Distribution;
import io.lexora.domain.billing.PaymentMeans;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdvanceRequestTest {

    private static final FolderId FOLDER = FolderId.of(1);
    private static final CustomerId ALICE = CustomerId.of(10);
    private static final AdvanceRequestId REQUEST_ID = AdvanceRequestId.of(7);

    private final AdvanceRequest request = AdvanceRequest.restore(REQUEST_ID, FOLDER, Money.of("800"), Money.ZERO, null,
            new AdvanceRequestTerms(LocalDate.of(2026, 1, 5), null, 10, Money.of("500"),
                    new Distribution(Map.of(ALICE, Money.of("500"))), null),
            null, List.of());

    @Test
    void is_settled_by_its_advances_within_the_tolerance() {
        Advance first = advance(1, "200.00", LocalDate.of(2026, 1, 10));
        Advance second = advance(2, "299.50", LocalDate.of(2026, 1, 20));

        request.settleIfCovered(List.of(first));
        assertThat(request.isPaid()).isFalse();

        request.settleIfCovered(List.of(first, second));
        assertThat(request.paidOn()).isEqualTo(LocalDate.of(2026, 1, 20));
        assertThat(request.remainingSumFor(ALICE, List.of(first, second))).isEqualTo(Money.of("0.50"));
    }

    @Test
    void reopens_when_its_advances_no_longer_cover_it() {
        Advance first = advance(1, "200.00", LocalDate.of(2026, 1, 10));
        Advance second = advance(2, "300.00", LocalDate.of(2026, 1, 20));
        request.settleIfCovered(List.of(first, second));

        request.reopenIfUncovered(List.of(first));

        assertThat(request.isPaid()).isFalse();
    }

    @Test
    void ignores_advances_answering_other_requests() {
        Advance other = Advance.restore(AdvanceId.of(3), FOLDER, AdvanceRequestId.of(99), ALICE,
                LocalDate.of(2026, 1, 10), Money.of("500"), PaymentMeans.UNKNOWN, null);

        request.settleIfCovered(List.of(other));

        assertThat(request.isPaid()).isFalse();
        assertThat(request.paidSum(List.of(other))).isEqualTo(Money.ZERO);
    }

    @Test
    void is_invoiced_once_all_its_advances_were_deducted() {
        Advance first = advance(1, "200.00", LocalDate.of(2026, 1, 10));
        Advance second = advance(2, "300.00", LocalDate.of(2026, 1, 20));
        first.deductOn(InvoiceId.of(1));

        assertThat(request.isInvoiced(List.of(first, second))).isFalse();
        second.deductOn(InvoiceId.of(1));
        assertThat(request.isInvoiced(List.of(first, second))).isTrue();
        assertThat(request.isInvoiced(List.of())).isFalse();
    }

    private static Advance advance(long id, String amount, LocalDate date) {
        return Advance.restore(AdvanceId.of(id), FOLDER, REQUEST_ID, ALICE, date, Money.of(amount),
                PaymentMeans.UNKNOWN, null);
    }
}
