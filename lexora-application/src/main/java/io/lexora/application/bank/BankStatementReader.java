package io.lexora.application.bank;

/** Reads a bank notification file; implemented by the infrastructure (ISO 20022 camt.054). */
public interface BankStatementReader {

    /** @throws io.lexora.domain.shared.DomainException when the file is not a readable notification */
    BankStatement read(String content);
}
