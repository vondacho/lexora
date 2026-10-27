package ch.conciliations.mabecoju.infrastructure.web.invoicing;

import ch.conciliations.mabecoju.application.document.DocumentUseCases;
import ch.conciliations.mabecoju.application.document.PrintOptions;
import ch.conciliations.mabecoju.application.document.PrintedDocument;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.template.DocumentModelId;
import ch.conciliations.mabecoju.domain.user.Username;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.PdfResponses;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.DocumentsApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.PrintInputDto;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/** PDF documents, generated on demand and never stored. */
@RestController
class DocumentController implements DocumentsApi {

    private final DocumentUseCases documents;

    DocumentController(DocumentUseCases documents) {
        this.documents = documents;
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_DISPLAY)
    public Resource printInvoice(Long id, PrintInputDto input) {
        return pdf(documents.printInvoice(InvoiceId.of(id), options(input), issuer()));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_DISPLAY)
    public Resource printInvoiceRecall(Long id, Long recallId, PrintInputDto input) {
        return pdf(documents.printInvoiceRecall(InvoiceId.of(id), RecallId.of(recallId), options(input), issuer()));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_DISPLAY)
    public Resource printAdvanceRequest(Long id, PrintInputDto input) {
        return pdf(documents.printAdvanceRequest(AdvanceRequestId.of(id), options(input), issuer()));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_DISPLAY)
    public Resource printAdvanceRequestRecall(Long id, Long recallId, PrintInputDto input) {
        return pdf(documents.printAdvanceRequestRecall(AdvanceRequestId.of(id), RecallId.of(recallId), options(input),
                issuer()));
    }

    private static PrintOptions options(PrintInputDto input) {
        return new PrintOptions(DocumentModelId.of(input.getModelId()), input.getContent(),
                Json.intOrZero(input.getAttachments()), Json.isTrue(input.getMergeAddresses()));
    }

    private static Username issuer() {
        return Username.of(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private static Resource pdf(PrintedDocument document) {
        return PdfResponses.inline(document);
    }
}
