package ch.conciliations.mabecoju.domain.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTest {

    @Test
    void extracts_the_reference_from_a_scanned_payment_slip_line() {
        assertThat(Ticket.decodeReference("0100000123456> 210000000003139471430009017+ 010001628>"))
                .isEqualTo("210000000003139471430009017");
    }

    @Test
    void keeps_a_typed_reference_without_spaces() {
        assertThat(Ticket.decodeReference("21 00000 00003 13947")).isEqualTo("21000000000313947");
    }

    @Test
    void recognises_cash_tickets() {
        assertThat(Ticket.issue(Ticket.CASH, null, null).isCash()).isTrue();
    }
}
