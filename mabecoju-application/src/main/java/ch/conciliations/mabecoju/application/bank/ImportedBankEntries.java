package ch.conciliations.mabecoju.application.bank;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;

/**
 * Bank transactions already imported, by the reference the bank gives each of them: a notification imported twice
 * (or two overlapping notifications) does not book a payment twice.
 */
public interface ImportedBankEntries {

    boolean contains(String entryReference);

    void add(String entryReference, LocalDate date, Money amount, String iban);
}
