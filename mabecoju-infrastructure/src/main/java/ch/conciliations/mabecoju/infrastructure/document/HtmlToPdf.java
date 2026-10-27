package ch.conciliations.mabecoju.infrastructure.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Turns an HTML page into a PDF with OpenHTMLtoPDF, with the configured font and no external resources. */
@Component
class HtmlToPdf {

    private final DocumentProperties properties;

    HtmlToPdf(DocumentProperties properties) {
        this.properties = properties;
    }

    /** CSS font-family of the documents. */
    String fontFamily() {
        return hasFont() ? "'" + properties.fontFamily() + "', sans-serif" : "sans-serif";
    }

    byte[] render(String html) {
        Document page = Jsoup.parse(html);
        page.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder()
                .useFastMode()
                .withW3cDocument(new W3CDom().fromJsoup(page), "/")
                // only embedded (data:) images may be loaded: no file or network access from templates
                .useUriResolver((base, uri) -> uri != null && uri.startsWith("data:") ? uri : null)
                .withProducer("mabecoju")
                .toStream(out);
        if (hasFont()) {
            builder.useFont(new File(properties.fontFile()), properties.fontFamily());
        }
        try {
            builder.run();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the PDF", e);
        }
        return out.toByteArray();
    }

    private boolean hasFont() {
        return properties.fontFile() != null && !properties.fontFile().isBlank();
    }
}
