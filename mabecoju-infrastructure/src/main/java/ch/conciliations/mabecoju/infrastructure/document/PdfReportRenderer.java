package ch.conciliations.mabecoju.infrastructure.document;

import ch.conciliations.mabecoju.application.document.DocumentFormats;
import ch.conciliations.mabecoju.application.document.TemplateText;
import ch.conciliations.mabecoju.application.reporting.ExpenseReport;
import ch.conciliations.mabecoju.application.reporting.IncomeReport;
import ch.conciliations.mabecoju.application.reporting.ReportRenderer;
import ch.conciliations.mabecoju.domain.booking.OutgoingPayment;
import ch.conciliations.mabecoju.domain.shared.Money;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Period statements laid out like the legacy "relevés": incomes on the first page (summary with the VAT owed,
 * invoice payments, advances), expenses from the second page. No letterhead, page numbers at the bottom.
 */
@Component
class PdfReportRenderer implements ReportRenderer {

    private final HtmlToPdf htmlToPdf;

    PdfReportRenderer(HtmlToPdf htmlToPdf) {
        this.htmlToPdf = htmlToPdf;
    }

    @Override
    public byte[] render(IncomeReport incomes, ExpenseReport expenses) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"/><style>").append(css()).append("</style></head><body>");
        String period = "période du " + DocumentFormats.shortDate(incomes.period().from()) + " au "
                + DocumentFormats.shortDate(incomes.period().to());

        html.append("<h1>Relevé des entrées</h1><div class=\"period\">").append(period).append("</div>");
        int count = incomes.invoicePayments().size() + incomes.advances().size();
        html.append("<div class=\"summary\">versements (").append(count).append("), encaissé ")
                .append(amount(incomes.total())).append(", TVA due (")
                .append(incomes.vatRate().percent().stripTrailingZeros().toPlainString()).append(" %) ")
                .append(amount(incomes.vatDue())).append("</div>");
        incomes(html, "Factures", incomes.invoicePayments(), incomes.invoicePaymentsTotal());
        incomes(html, "Avances", incomes.advances(), incomes.advancesTotal());

        html.append("<h1 class=\"new-page\">Relevé des sorties</h1><div class=\"period\">").append(period).append("</div>");
        html.append("<div class=\"summary\">paiements (").append(expenses.payments().size()).append("), total ")
                .append(amount(expenses.total())).append(", dont tva ").append(amount(expenses.taxTotal()))
                .append("</div>");
        html.append("<table><tr class=\"head\"><td>date</td><td>créancier</td><td>libellé</td><td>type</td>")
                .append("<td class=\"num\">tva</td><td class=\"num\">montant</td></tr>");
        for (OutgoingPayment p : expenses.payments()) {
            html.append("<tr><td>").append(DocumentFormats.shortDate(p.details().paidOn())).append("</td><td>")
                    .append(esc(p.details().creditor())).append("</td><td>").append(esc(p.details().title()))
                    .append("</td><td>").append(kind(p)).append("</td><td class=\"num\">").append(amount(p.tax()))
                    .append("</td><td class=\"num\">").append(amount(p.amount())).append("</td></tr>");
        }
        html.append("<tr class=\"total\"><td colspan=\"4\">total</td><td class=\"num\">").append(amount(expenses.taxTotal()))
                .append("</td><td class=\"num\">").append(amount(expenses.total())).append("</td></tr></table>");
        return htmlToPdf.render(html.append("</body></html>").toString());
    }

    private void incomes(StringBuilder html, String title, List<IncomeReport.IncomeLine> lines, Money total) {
        html.append("<h2>").append(title).append(" (").append(lines.size()).append("), encaissé ")
                .append(amount(total)).append("</h2>");
        html.append("<table><tr class=\"head\"><td>date</td><td>dossier</td><td>bvr / qr</td><td>iban</td>")
                .append("<td>comptant</td><td class=\"num\">montant</td></tr>");
        for (IncomeReport.IncomeLine l : lines) {
            html.append("<tr><td>").append(DocumentFormats.shortDate(l.date())).append("</td><td>")
                    .append(l.folderName() == null ? "inconnu" : esc(l.folderName())).append("</td><td class=\"ref\">")
                    .append(esc(l.ticketNumber())).append("</td><td>").append(esc(l.iban())).append("</td><td>")
                    .append(l.cash() ? "oui" : "non").append("</td><td class=\"num\">").append(amount(l.amount()))
                    .append("</td></tr>");
        }
        html.append("<tr class=\"total\"><td colspan=\"5\">total</td><td class=\"num\">").append(amount(total))
                .append("</td></tr></table>");
    }

    private static String kind(OutgoingPayment payment) {
        return switch (payment.details().kind()) {
            case BANK -> "normal";
            case EXPENSE -> "frais";
            case PERMANENT -> "ordre";
            case UNDEFINED -> "";
        };
    }

    private String css() {
        return """
                @page { size: A4; margin: 18mm 12mm 18mm 15mm;
                  @bottom-right { content: "page " counter(page) " / " counter(pages); font-family: %1$s; font-size: 8pt; } }
                body { font-family: %1$s; font-size: 9pt; }
                h1 { font-size: 13pt; margin: 0 0 1mm 0; }
                h2 { font-size: 10pt; margin: 6mm 0 1mm 0; }
                .new-page { page-break-before: always; }
                .period { margin-bottom: 3mm; }
                .summary { font-weight: bold; padding: 1.5mm 0; border-top: 0.5pt solid #000; border-bottom: 0.5pt solid #000; }
                table { border-collapse: collapse; width: 100%%; -fs-table-paginate: paginate; }
                td { padding: 0.5mm 1mm 0.5mm 0; vertical-align: top; }
                .head td { font-weight: bold; border-bottom: 0.3pt solid #000; }
                .total td { font-weight: bold; border-top: 0.3pt solid #000; }
                .num { text-align: right; }
                .ref { font-size: 7.5pt; }
                """.formatted(htmlToPdf.fontFamily());
    }

    private static String amount(Money money) {
        return money == null ? "" : DocumentFormats.amount(money);
    }

    private static String esc(String text) {
        return TemplateText.escape(text);
    }
}
