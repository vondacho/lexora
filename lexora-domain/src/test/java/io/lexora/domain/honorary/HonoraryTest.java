package io.lexora.domain.honorary;

import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HonoraryTest {

    @Test
    void amount_is_time_at_the_hourly_rate_plus_expenses() {
        Honorary honorary = Honorary.record(FolderId.of(1), new HonoraryWork(LocalDate.of(2026, 2, 1), "Meeting", 90,
                Money.of("200"), List.of(new Expense(Money.of("12.50"), "Travel"))));

        assertThat(honorary.amount()).isEqualTo(Money.of("312.50"));
        assertThat(honorary.isInvoiced()).isFalse();

        honorary.includeIn(InvoiceId.of(3));
        assertThat(honorary.isInvoiced()).isTrue();
        honorary.release();
        assertThat(honorary.isInvoiced()).isFalse();
    }

    @Test
    void an_invoiced_honorary_cannot_be_revised() {
        HonoraryWork work = new HonoraryWork(LocalDate.of(2026, 2, 1), "Meeting", 60, Money.of("200"), List.of());
        Honorary honorary = Honorary.record(FolderId.of(1), work);
        honorary.includeIn(InvoiceId.of(3));

        assertThatThrownBy(() -> honorary.revise(work)).isInstanceOf(DomainException.class);
        assertThat(honorary.amount()).isEqualTo(Money.of("200.00"));
    }
}
