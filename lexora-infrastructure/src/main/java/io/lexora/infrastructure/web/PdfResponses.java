package io.lexora.infrastructure.web;

import io.lexora.application.document.PrintedDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;

/** Sends a generated PDF, displayed inline by browsers under its file name. */
public final class PdfResponses {

    private PdfResponses() {
    }

    /**
     * The body is always declared {@code application/pdf}. The PDF operations also list the JSON types of their
     * error responses, and content negotiation would otherwise label the PDF with whatever the client accepts first
     * (generated clients send {@code Accept: application/problem+json}): browsers then refuse to render it.
     */
    public static Resource inline(PrintedDocument document) {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        var response = attributes.getResponse();
        if (response != null) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename(document.fileName(), StandardCharsets.UTF_8).build().toString());
        }
        return new ByteArrayResource(document.pdf());
    }
}
