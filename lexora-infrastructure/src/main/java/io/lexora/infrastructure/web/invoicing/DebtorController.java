package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.debtor.CustomerStatement;
import io.lexora.application.debtor.DebtorUseCases;
import io.lexora.application.debtor.DebtorsOverview;
import io.lexora.application.debtor.OpenItem;
import io.lexora.application.debtor.ReceivePaymentsCommand;
import io.lexora.application.debtor.TicketLookup;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.ticket.TicketId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.DebtorsApi;
import io.lexora.infrastructure.web.api.model.CustomerStatementDto;
import io.lexora.infrastructure.web.api.model.DebtorsOverviewDto;
import io.lexora.infrastructure.web.api.model.FolderToInvoiceDto;
import io.lexora.infrastructure.web.api.model.OpenDebtDto;
import io.lexora.infrastructure.web.api.model.OpenItemDto;
import io.lexora.infrastructure.web.api.model.PaymentLineDto;
import io.lexora.infrastructure.web.api.model.ReceiptInputDto;
import io.lexora.infrastructure.web.api.model.TicketLookupDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import static io.lexora.infrastructure.web.Json.amount;
import static io.lexora.infrastructure.web.Json.map;

@RestController
class DebtorController implements DebtorsApi {

    private final DebtorUseCases debtors;

    DebtorController(DebtorUseCases debtors) {
        this.debtors = debtors;
    }

    @Override
    @PreAuthorize(Permissions.DEBTORS_OPEN)
    public DebtorsOverviewDto getDebtorsOverview() {
        DebtorsOverview o = debtors.overview();
        return new DebtorsOverviewDto()
                .foldersToInvoice(map(o.foldersToInvoice(), f -> new FolderToInvoiceDto().folderId(f.folderId().value())
                        .folderName(f.folderName()).toBeInvoiced(amount(f.toBeInvoiced()))
                        .available(amount(f.available())).uncovered(amount(f.uncovered()))))
                .foldersTotal(amount(o.foldersTotal()))
                .openInvoices(map(o.openInvoices(), DebtorController::toDto)).invoicesTotal(amount(o.invoicesTotal()))
                .openRequests(map(o.openRequests(), DebtorController::toDto)).requestsTotal(amount(o.requestsTotal()));
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_SEARCH)
    public TicketLookupDto lookupTicket(String reference) {
        TicketLookup l = debtors.lookupTicket(reference);
        return new TicketLookupDto().ticketId(l.ticket().id().value()).number(l.ticket().number())
                .folderId(l.folder() == null ? null : l.folder().id().value())
                .folderName(l.folder() == null ? null : l.folder().name())
                .customerId(l.customer() == null ? null : l.customer().id().value())
                .customerName(l.customer() == null ? null : l.customer().fullName())
                .invoiceId(Json.id(l.ticket().invoiceId())).requestId(Json.id(l.ticket().requestId()))
                .payments(map(l.payments(), p -> new PaymentLineDto().id(p.payment().id().value())
                        .invoiceId(p.invoiceId().value()).folderId(p.folderId().value()).date(p.payment().date())
                        .amount(amount(p.payment().amount())).customerId(Json.id(p.payment().customerId()))))
                .advances(map(l.advances(), InvoicingMapper::toDto))
                .statement(toDto(l.statement()));
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_RECEIPT)
    public CustomerStatementDto getCustomerStatement(Long folderId, Long customerId) {
        return toDto(debtors.statement(FolderId.of(folderId), CustomerId.of(customerId)));
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_RECEIPT)
    public CustomerStatementDto receivePayments(Long folderId, Long customerId, ReceiptInputDto input) {
        return toDto(debtors.receivePayments(new ReceivePaymentsCommand(FolderId.of(folderId), CustomerId.of(customerId),
                input.getTicketId() == null ? null : TicketId.of(input.getTicketId()),
                map(input.getInvoices(), l -> new ReceivePaymentsCommand.InvoiceLine(InvoiceId.of(l.getDocumentId()),
                        l.getDate(), Json.money(l.getAmount()), l.getIban(), Json.isTrue(l.getCash()))),
                map(input.getRequests(), l -> new ReceivePaymentsCommand.RequestLine(
                        AdvanceRequestId.of(l.getDocumentId()), l.getDate(), Json.money(l.getAmount()), l.getIban(),
                        Json.isTrue(l.getCash()))))));
    }

    private static CustomerStatementDto toDto(CustomerStatement s) {
        if (s == null) {
            return null;
        }
        return new CustomerStatementDto().folderId(s.folder().id().value()).folderName(s.folder().name())
                .customerId(s.customer().id().value()).customerName(s.customer().fullName())
                .invoices(map(s.invoices(), DebtorController::toDto)).requests(map(s.requests(), DebtorController::toDto))
                .totalRemaining(amount(s.totalRemaining()));
    }

    private static OpenItemDto toDto(OpenItem i) {
        return new OpenItemDto().documentId(i.documentId()).date(i.date()).dueDate(i.dueDate())
                .share(amount(i.share())).paid(amount(i.paid())).remaining(amount(i.remaining()));
    }

    private static OpenDebtDto toDto(DebtorsOverview.OpenDebt d) {
        return new OpenDebtDto().documentId(d.documentId()).folderId(d.folderId().value()).folderName(d.folderName())
                .date(d.date()).dueDate(d.dueDate()).daysOverdue(d.daysOverdue()).amount(amount(d.amount()))
                .remaining(amount(d.remaining())).recallCount(d.recallCount()).lastRecallDate(d.lastRecallDate());
    }
}
