package ch.conciliations.mabecoju.application.bank;

import java.time.LocalDate;
import java.util.List;

/**
 * Credits of a bank notification file (camt.054). Debits are not imported: office payments are recorded by hand.
 *
 * @param skippedDebits number of debit transactions left out
 */
public record BankStatement(String accountIban, LocalDate from, LocalDate to, List<BankCredit> credits,
                            int skippedDebits) {

    public BankStatement {
        credits = List.copyOf(credits);
    }
}
