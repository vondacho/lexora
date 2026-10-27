package io.lexora.infrastructure.persistence.bank;

import io.lexora.application.bank.ImportedBankEntries;
import io.lexora.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
class ImportedBankEntriesAdapter implements ImportedBankEntries {

    private final BankEntryJpaRepository jpa;

    ImportedBankEntriesAdapter(BankEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean contains(String entryReference) {
        return entryReference != null && jpa.existsByReference(entryReference);
    }

    @Override
    public void add(String entryReference, LocalDate date, Money amount, String iban) {
        BankEntryEntity e = new BankEntryEntity();
        e.reference = entryReference;
        e.date = date;
        e.amount = amount.amount();
        e.iban = iban;
        jpa.saveAndFlush(e);
    }
}
