package ch.conciliations.mabecoju.infrastructure.web.invoicing;

import ch.conciliations.mabecoju.application.bank.BankEntryMatch;
import ch.conciliations.mabecoju.application.bank.BankImportUseCases;
import ch.conciliations.mabecoju.application.bank.BankReceipt;
import ch.conciliations.mabecoju.application.bank.BankReceiptResult;
import ch.conciliations.mabecoju.domain.billing.PaymentReference.DocumentKind;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.BankStatementsApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankDocumentKindDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankEntryDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankReceiptInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankReceiptResultDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankReceiptsInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankReceiptsResultDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankStatementAnalysisDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankStatementInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BankTargetDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import static ch.conciliations.mabecoju.infrastructure.web.Json.amount;

@RestController
class BankStatementController implements BankStatementsApi {

    private final BankImportUseCases bank;

    BankStatementController(BankImportUseCases bank) {
        this.bank = bank;
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_RECEIPT)
    public BankStatementAnalysisDto analyseBankStatement(BankStatementInputDto input) {
        BankImportUseCases.Analysis analysis = bank.analyse(input.getContent());
        return new BankStatementAnalysisDto()
                .accountIban(analysis.statement().accountIban())
                .from(analysis.statement().from())
                .to(analysis.statement().to())
                .skippedDebits(analysis.statement().skippedDebits())
                .entries(Json.map(analysis.entries(), BankStatementController::toDto));
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_RECEIPT)
    public BankReceiptsResultDto recordBankReceipts(BankReceiptsInputDto input) {
        return new BankReceiptsResultDto().results(Json.map(
                bank.record(Json.map(input.getReceipts(), BankStatementController::toReceipt)),
                BankStatementController::toDto));
    }

    private static BankEntryDto toDto(BankEntryMatch m) {
        var c = m.credit();
        BankEntryMatch.Target t = m.target();
        return new BankEntryDto()
                .entryReference(c.entryReference()).date(c.date()).amount(amount(c.amount())).currency(c.currency())
                .reference(c.reference()).debtorName(c.debtorName()).debtorIban(c.debtorIban()).remittance(c.remittance())
                .status(BankEntryDto.StatusEnum.valueOf(m.status().name()))
                .problem(BankEntryDto.ProblemEnum.valueOf(m.problem().name()))
                .target(t == null ? null : new BankTargetDto()
                        .kind(BankDocumentKindDto.valueOf(t.kind().name())).documentId(t.documentId())
                        .folderId(t.folderId()).folderName(t.folderName())
                        .customerId(t.customerId()).customerName(t.customerName()))
                .remaining(m.remaining() == null ? null : amount(m.remaining()));
    }

    private static BankReceipt toReceipt(BankReceiptInputDto in) {
        return new BankReceipt(DocumentKind.valueOf(in.getKind().name()), in.getDocumentId(), in.getCustomerId(),
                in.getDate(), Json.money(in.getAmount()), in.getIban(), in.getReference(), in.getEntryReference());
    }

    private static BankReceiptResultDto toDto(BankReceiptResult r) {
        return new BankReceiptResultDto().recorded(r.recorded()).settled(r.settled()).advance(amount(r.advance()));
    }
}
