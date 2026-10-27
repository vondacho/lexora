package io.lexora.domain.billing;

import io.lexora.domain.ticket.TicketId;

/**
 * How a debtor paid.
 *
 * @param ticketId payment slip the payment was matched with, if any
 * @param iban     account the payment came from, if known
 * @param cash     true when paid in cash
 */
public record PaymentMeans(TicketId ticketId, String iban, boolean cash) {

    public static final PaymentMeans UNKNOWN = new PaymentMeans(null, null, false);
}
