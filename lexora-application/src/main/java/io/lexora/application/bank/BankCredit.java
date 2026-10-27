package io.lexora.application.bank;

import io.lexora.domain.shared.Money;

import java.time.LocalDate;

/**
 * Money received on the office account, as the bank reports it (one transaction of a camt.054 notification).
 *
 * @param entryReference reference of the bank for the transaction, when given
 * @param reference      QR or creditor reference of the payment, null without
 * @param remittance     free text of the payer, null without
 */
public record BankCredit(
        String entryReference,
        LocalDate date,
        Money amount,
        String currency,
        String reference,
        String debtorName,
        String debtorIban,
        String remittance) {
}
