package io.lexora.infrastructure.document;

import io.lexora.application.document.DocumentFormats;
import io.lexora.application.document.DocumentRenderer;
import io.lexora.application.document.PrintJob;
import io.lexora.application.document.TemplateText;
import io.lexora.domain.billing.PaymentReference;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Money;
import net.codecrete.qrbill.canvas.PDFCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.ValidationResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

/**
 * Renders documents with OpenHTMLtoPDF from an HTML page laid out like the legacy TCPDF documents, then appends a
 * QR-bill payment part to each copy that has something to pay.
 */
@Component
class PdfDocumentRenderer implements DocumentRenderer {

    /** User templates may format text, but not load images or run scripts. */
    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed().removeTags("img")
            .addAttributes(":all", "style", "align");

    private final DocumentProperties properties;
    private final HtmlToPdf htmlToPdf;
    private final String headerImage;
    private final String footerImage;

    PdfDocumentRenderer(DocumentProperties properties, HtmlToPdf htmlToPdf) {
        this.properties = properties;
        this.htmlToPdf = htmlToPdf;
        this.headerImage = dataUri(properties.headerImage());
        this.footerImage = dataUri(properties.footerImage());
    }

    @Override
    public PaymentReference.Style paymentReferenceStyle() {
        return properties.qrBill().isConfigured()
                ? PaymentReference.styleFor(properties.qrBill().iban())
                : PaymentReference.Style.NONE;
    }

    @Override
    public byte[] render(PrintJob job) {
        try (PDDocument result = new PDDocument()) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (PrintJob.Copy copy : job.copies()) {
                try (PDDocument copyDocument = Loader.loadPDF(htmlToPdf.render(new Page(job, copy).html()))) {
                    if (copy.paymentSlip() != null) {
                        appendQrBill(copyDocument, copy.paymentSlip());
                    }
                    merger.appendDocument(result, copyDocument);
                }
            }
            PDDocumentInformation info = result.getDocumentInformation();
            info.setTitle(job.fileName());
            info.setSubject(job.subject());
            info.setAuthor(properties.author());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            result.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render " + job.fileName(), e);
        }
    }

    // ----- HTML -------------------------------------------------------------------------------------------------

    /** One copy: the pages of the template, all addressed to the copy's recipient first. */
    private final class Page {

        private final PrintJob job;
        private final PrintJob.Copy copy;
        private final StringBuilder html = new StringBuilder();

        Page(PrintJob job, PrintJob.Copy copy) {
            this.job = job;
            this.copy = copy;
        }

        String html() {
            html.append("<html><head><meta charset=\"UTF-8\"/><style>").append(css()).append("</style></head><body>");
            html.append("<div class=\"header\"><img src=\"").append(headerImage).append("\"/></div>");
            html.append("<div class=\"footer\"><img src=\"").append(footerImage).append("\"/></div>");
            if (job.attachments() > 0) {
                html.append("<div class=\"attachments\"><u>").append(job.attachments() > 1 ? "Annexes" : "Annexe")
                        .append("</u> : ment.</div>");
            }
            boolean first = true;
            for (PrintJob.PageKind kind : job.pages()) {
                html.append(first ? "<div>" : "<div class=\"new-page\">");
                addresses();
                switch (kind) {
                    case LETTER -> letter();
                    case INVOICE_STATEMENT -> statement(job.statement());
                }
                html.append("</div>");
                first = false;
            }
            return html.append("</body></html>").toString();
        }

        private String css() {
            String font = htmlToPdf.fontFamily();
            return """
                    @page { size: A4; margin: 45mm 10mm 28mm 18mm;
                      @top-right { content: element(header); }
                      @bottom-left { content: element(footer); } }
                    @page :first { @bottom-left { content: element(attachments) element(footer); } }
                    body { font-family: %s; font-size: 10pt; }
                    .header { position: running(header); text-align: right; }
                    .header img { height: 30mm; }
                    .footer { position: running(footer); }
                    .footer img { width: 182mm; }
                    .attachments { position: running(attachments); font-size: 9pt; margin-bottom: 2mm; }
                    .new-page { page-break-before: always; }
                    table { border-collapse: collapse; width: 100%%; }
                    td { vertical-align: top; padding: 0; }
                    .address { margin-bottom: 8mm; }
                    .spacer { width: 105mm; }
                    .small { font-size: 8pt; }
                    .title { font-weight: bold; margin: 8mm 0 5mm 0; }
                    .signature { margin-top: 25mm; margin-left: 105mm; }
                    .statement { margin-top: 8mm; font-size: 9pt; }
                    .statement td { padding: 0.6mm 0; }
                    .num { text-align: right; }
                    .head td { font-weight: bold; border-bottom: 0.3pt solid #000; }
                    .total td { font-weight: bold; }
                    .gap td { height: 3mm; }
                    """.formatted(font);
        }

        private void addresses() {
            for (List<String> block : copy.addressBlocks()) {
                html.append("<table class=\"address\"><tr><td class=\"spacer\"></td><td>");
                html.append(String.join("<br/>", block.stream().map(TemplateText::escape).toList()));
                html.append("</td></tr></table>");
            }
            html.append("<table><tr><td class=\"spacer small\">N.réf. : ").append(esc(job.reference()));
            html.append("<br/>No TVA : ").append(esc(job.vatNumber())).append("</td><td>");
            html.append(esc(properties.city())).append(", le ").append(DocumentFormats.longDate(job.date()));
            html.append("</td></tr></table>");
        }

        private void letter() {
            html.append("<div class=\"title\">").append(esc(job.modelTitle())).append("</div>");
            String body = job.htmlContent()
                    ? Jsoup.clean(job.content(), CONTENT_SAFELIST)
                    : job.content().replace("\r\n", "\n").replace("\n", "<br/>");
            html.append("<div>").append(body).append("</div>");
            html.append("<div class=\"signature\">").append(esc(properties.signature())).append("</div>");
        }

        private void statement(PrintJob.InvoiceStatement s) {
            html.append("<table class=\"statement\">");
            html.append("<tr class=\"head\"><td style=\"width:18mm\">Facture</td><td></td>")
                    .append("<td class=\"num\" style=\"width:16mm\">minutes</td><td class=\"num\" style=\"width:18mm\">tarif</td>")
                    .append("<td class=\"num\" style=\"width:18mm\">frais</td><td class=\"num\" style=\"width:22mm\">montant</td></tr>");
            for (PrintJob.InvoiceStatement.Line l : s.honoraries()) {
                row(DocumentFormats.shortDate(l.date()), esc(l.description()), String.valueOf(l.minutes()),
                        amount(l.hourlyRate()), amount(l.expenses()), amount(l.amount()), false);
            }
            gap();
            summary("honoraires", null, s.honorariesSum(), false);
            int total = 1;
            if (s.discount() != null) {
                summary("remise", null, s.discount(), true);
                summary("total " + total++, null, s.honorariesSum().minus(s.discount()), true);
            }
            summary("tva", s.taxRatePercent().stripTrailingZeros().toPlainString() + " %", s.tax(), false);
            summary("total " + total, null, s.total(), true);
            for (PrintJob.InvoiceStatement.Balance b : s.balances()) {
                gap();
                if (b.customerName() != null) {
                    row("", esc(b.customerName()), "", "", "à charge", amount(b.toBear()), false);
                }
                for (PrintJob.InvoiceStatement.AdvanceLine a : b.advances()) {
                    row(DocumentFormats.shortDate(a.date()), "acompte", "", "", "", amount(a.amount()), false);
                }
                summary("acomptes", null, b.advancesSum(), false);
                summary("solde dû", null, b.due(), true);
            }
            gap();
            row("", "", "", "", "", "", false);
            html.append("<tr><td></td><td></td><td colspan=\"4\">paiement à ").append(s.delayDays())
                    .append(" jours<br/>avec mes remerciements</td></tr>");
            html.append("</table>");
        }

        private void summary(String label, String rate, Money value, boolean bold) {
            row("", "", label, "", rate == null ? "" : rate, amount(value), bold);
        }

        private void row(String c1, String c2, String c3, String c4, String c5, String c6, boolean bold) {
            html.append(bold ? "<tr class=\"total\">" : "<tr>")
                    .append("<td>").append(c1).append("</td><td>").append(c2).append("</td>")
                    .append("<td class=\"num\">").append(c3).append("</td><td class=\"num\">").append(c4).append("</td>")
                    .append("<td class=\"num\">").append(c5).append("</td><td class=\"num\">").append(c6).append("</td></tr>");
        }

        private void gap() {
            html.append("<tr class=\"gap\"><td colspan=\"6\"></td></tr>");
        }

        private String amount(Money money) {
            return money == null ? "" : DocumentFormats.amount(money);
        }

        private String esc(String text) {
            return TemplateText.escape(text);
        }
    }

    // ----- QR-bill ---------------------------------------------------------------------------------------------

    private void appendQrBill(PDDocument document, PrintJob.PaymentSlip slip) throws IOException {
        DocumentProperties.QrBill creditor = properties.qrBill();
        Bill bill = new Bill();
        bill.setAccount(creditor.iban());
        bill.setCurrency("CHF");
        bill.setAmount(slip.amount().amount());
        bill.setCreditor(address(creditor.creditorName(), creditor.street(), creditor.houseNumber(),
                creditor.postalCode(), creditor.town(), creditor.country()));
        bill.setReference(slip.reference());
        bill.setUnstructuredMessage(slip.message());
        if (notBlank(slip.debtorName()) && notBlank(slip.debtorPostalCode()) && notBlank(slip.debtorTown())) {
            bill.setDebtor(address(slip.debtorName(), slip.debtorStreet(), null, slip.debtorPostalCode(),
                    slip.debtorTown(), "CH"));
        }
        bill.getFormat().setLanguage(Language.FR);
        ValidationResult validation = QRBill.validate(bill);
        if (validation.hasErrors()) {
            throw new DomainException(ErrorCode.QR_BILL_INVALID, "Invalid QR-bill: " + validation.getDescription())
                    .with("reason", validation.getDescription());
        }
        try (PDFCanvas canvas = new PDFCanvas(document, PDFCanvas.NEW_PAGE_AT_END)) {
            QRBill.draw(validation.getCleanedBill(), canvas);
        }
    }

    private static Address address(String name, String street, String houseNumber, String postalCode, String town,
                                   String country) {
        Address address = new Address();
        address.setName(name);
        address.setStreet(street);
        address.setHouseNo(houseNumber);
        address.setPostalCode(postalCode);
        address.setTown(town);
        address.setCountryCode(country == null || country.isBlank() ? "CH" : country);
        return address;
    }

    private static boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    private static String dataUri(Resource image) {
        if (image == null || !image.exists()) {
            return "";
        }
        try (InputStream in = image.getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + image, e);
        }
    }
}
