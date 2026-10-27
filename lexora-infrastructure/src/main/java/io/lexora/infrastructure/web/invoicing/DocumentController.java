package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.document.DocumentUseCases;
import io.lexora.application.document.PrintOptions;
import io.lexora.application.document.PrintedDocument;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.billing.RecallId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.template.DocumentModelId;
import io.lexora.domain.user.Username;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.PdfResponses;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.DocumentsApi;
import io.lexora.infrastructure.web.api.model.PrintInputDto;
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
