package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.invoice.InvoiceDraft;
import io.lexora.application.invoice.InvoiceUseCases;
import io.lexora.application.invoice.RecordPaymentCommand;
import io.lexora.domain.billing.RecallId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.Invoice;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.invoice.PaymentId;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.InvoicesApi;
import io.lexora.infrastructure.web.api.model.AdvanceDto;
import io.lexora.infrastructure.web.api.model.DateInputDto;
import io.lexora.infrastructure.web.api.model.HonoraryDto;
import io.lexora.infrastructure.web.api.model.InvoiceDraftDto;
import io.lexora.infrastructure.web.api.model.InvoiceDto;
import io.lexora.infrastructure.web.api.model.InvoicePaymentInputDto;
import io.lexora.infrastructure.web.api.model.InvoiceTermsInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class InvoiceController implements InvoicesApi {

    private final InvoiceUseCases invoices;

    InvoiceController(InvoiceUseCases invoices) {
        this.invoices = invoices;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_INVOICES)
    public List<InvoiceDto> listFolderInvoices(Long folderId) {
        return Json.map(invoices.listInvoices(FolderId.of(folderId)), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_CREATE)
    public InvoiceDraftDto prepareInvoice(Long folderId) {
        InvoiceDraft d = invoices.prepareInvoice(FolderId.of(folderId));
        return new InvoiceDraftDto().folderId(d.folderId().value())
                .honoraries(Json.map(d.honoraries(), InvoicingMapper::toDto))
                .advances(Json.map(d.advances(), InvoicingMapper::toDto))
                .honorariesSum(Json.amount(d.honorariesSum())).advancedSum(Json.amount(d.advancedSum()))
                .customers(Json.map(d.customers(), InvoicingMapper::toDto))
                .defaultDelayDays(d.defaultDelayDays()).defaultTaxRatePercent(Json.percent(d.defaultTaxRate()));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_CREATE)
    public InvoiceDto issueInvoice(Long folderId, InvoiceTermsInputDto input) {
        return InvoicingMapper.toDto(invoices.issueInvoice(FolderId.of(folderId), InvoicingMapper.toTerms(input)));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('invoicing:debtors_open', 'invoicing:debtors_payed')")
    public List<InvoiceDto> listInvoicesByStatus(String status) {
        List<Invoice> found = switch (status) {
            case "open" -> invoices.listOpenInvoices();
            case "paid" -> invoices.listPaidInvoices();
            default -> throw new IllegalArgumentException("status must be 'open' or 'paid'");
        };
        return Json.map(found, InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_DISPLAY)
    public InvoiceDto getInvoice(Long id) {
        return InvoicingMapper.toDto(invoices.getInvoice(InvoiceId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_DISPLAY)
    public List<HonoraryDto> listInvoicedHonoraries(Long id) {
        return Json.map(invoices.invoicedHonoraries(InvoiceId.of(id)), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_DISPLAY)
    public List<AdvanceDto> listDeductedAdvances(Long id) {
        return Json.map(invoices.deductedAdvances(InvoiceId.of(id)), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto reviseInvoice(Long id, InvoiceTermsInputDto input) {
        return InvoicingMapper.toDto(invoices.reviseInvoice(InvoiceId.of(id), InvoicingMapper.toTerms(input)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public void deleteInvoice(Long id) {
        invoices.deleteInvoice(InvoiceId.of(id));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto settleInvoice(Long id, DateInputDto input) {
        return InvoicingMapper.toDto(invoices.settleInvoice(InvoiceId.of(id), Json.dateOf(input)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto reopenInvoice(Long id) {
        return InvoicingMapper.toDto(invoices.reopenInvoice(InvoiceId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto recallInvoice(Long id, DateInputDto input) {
        return InvoicingMapper.toDto(invoices.recallInvoice(InvoiceId.of(id), Json.dateOf(input)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto cancelInvoiceRecall(Long id, Long recallId) {
        return InvoicingMapper.toDto(invoices.cancelRecall(InvoiceId.of(id), RecallId.of(recallId)));
    }

    @Override
    @PreAuthorize(Permissions.DEBTOR_RECEIPT)
    public InvoiceDto recordInvoicePayment(Long id, InvoicePaymentInputDto input) {
        return InvoicingMapper.toDto(invoices.recordPayment(InvoiceId.of(id), new RecordPaymentCommand(
                input.getCustomerId() == null ? null : CustomerId.of(input.getCustomerId()), input.getDate(),
                Json.money(input.getAmount()), Json.means(input.getTicketId(), input.getIban(), input.getCash()))));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public InvoiceDto cancelInvoicePayment(Long id, Long paymentId) {
        Invoice invoice = invoices.getInvoice(InvoiceId.of(id));
        boolean own = invoice.payments().stream().anyMatch(p -> p.id().value() == paymentId);
        if (!own) {
            throw new DomainException(ErrorCode.PAYMENT_NOT_OF_INVOICE, "Payment " + paymentId + " does not belong to invoice " + id)
                    .with("paymentId", paymentId)
                    .with("invoiceId", id);
        }
        return InvoicingMapper.toDto(invoices.cancelPayment(PaymentId.of(paymentId)));
    }
}
