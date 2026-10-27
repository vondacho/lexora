package io.lexora.application.document;

import io.lexora.application.common.Ids;
import io.lexora.domain.advance.Advance;
import io.lexora.domain.advance.AdvanceRepository;
import io.lexora.domain.advance.AdvanceRequest;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.advance.AdvanceRequestRepository;
import io.lexora.domain.billing.PaymentReference;
import io.lexora.domain.billing.Recall;
import io.lexora.domain.billing.RecallId;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerDetails;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.customer.CustomerRepository;
import io.lexora.domain.customer.PostalAddress;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.honorary.Expense;
import io.lexora.domain.honorary.Honorary;
import io.lexora.domain.honorary.HonoraryRepository;
import io.lexora.domain.invoice.Invoice;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.invoice.InvoiceRepository;
import io.lexora.domain.settings.SettingsRepository;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Strings;
import io.lexora.domain.template.DocumentModel;
import io.lexora.domain.template.DocumentModelRepository;
import io.lexora.domain.template.DocumentType;
import io.lexora.domain.template.PageLayout;
import io.lexora.domain.ticket.Ticket;
import io.lexora.domain.ticket.TicketRepository;
import io.lexora.domain.user.User;
import io.lexora.domain.user.UserRepository;
import io.lexora.domain.user.Username;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.lexora.application.document.DocumentFormats.amount;
import static io.lexora.application.document.DocumentFormats.longDate;
import static io.lexora.application.document.DocumentFormats.shortDate;

/**
 * Printing invoices, advance requests and their reminders from the letter templates, on demand.
 * <p>
 * Rules taken over from the legacy print actions: the recipients are the customers active at the document date
 * (at most three); each one receives a copy with his address first, unless the addresses are merged; a billing
 * customer owes the whole amount. Each copy gets a QR-bill for what its recipient still owes, with a reference
 * registered as a payment slip so that the payment is matched automatically.
 */
@Transactional
public class DocumentUseCases {

    private static final int MAX_RECIPIENTS = 3;

    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final HonoraryRepository honoraries;
    private final CustomerRepository customers;
    private final DocumentModelRepository models;
    private final SettingsRepository settings;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final DocumentRenderer renderer;

    public DocumentUseCases(InvoiceRepository invoices, AdvanceRequestRepository requests, AdvanceRepository advances,
                            HonoraryRepository honoraries, CustomerRepository customers, DocumentModelRepository models,
                            SettingsRepository settings, UserRepository users, TicketRepository tickets,
                            DocumentRenderer renderer) {
        this.invoices = invoices;
        this.requests = requests;
        this.advances = advances;
        this.honoraries = honoraries;
        this.customers = customers;
        this.models = models;
        this.settings = settings;
        this.users = users;
        this.tickets = tickets;
        this.renderer = renderer;
    }

    public PrintedDocument printInvoice(InvoiceId id, PrintOptions options, Username issuer) {
        Invoice invoice = loadInvoice(id);
        return print(invoiceSource(invoice, null), options, issuer);
    }

    public PrintedDocument printInvoiceRecall(InvoiceId id, RecallId recallId, PrintOptions options, Username issuer) {
        Invoice invoice = loadInvoice(id);
        return print(invoiceSource(invoice, findRecall(invoice.recalls(), recallId)), options, issuer);
    }

    public PrintedDocument printAdvanceRequest(AdvanceRequestId id, PrintOptions options, Username issuer) {
        AdvanceRequest request = loadRequest(id);
        return print(requestSource(request, null), options, issuer);
    }

    public PrintedDocument printAdvanceRequestRecall(AdvanceRequestId id, RecallId recallId, PrintOptions options,
                                                     Username issuer) {
        AdvanceRequest request = loadRequest(id);
        return print(requestSource(request, findRecall(request.recalls(), recallId)), options, issuer);
    }

    // ----- what is printed -------------------------------------------------------------------------------------

    /** The facts of the document to print, whatever its kind. */
    private record Source(
            PaymentReference.DocumentKind kind,
            long documentId,
            FolderId folderId,
            LocalDate documentDate,
            Recall recall,
            Recall previousRecall,
            int delayDays,
            LocalDate dueDate,
            String subject,
            String filePrefix,
            String paymentMessage,
            boolean hasDistribution,
            Function<CustomerId, Money> share,
            Function<CustomerId, Money> remainingFor,
            Money total,
            Money remainingTotal,
            PrintJob.InvoiceStatement statement,
            InvoiceId invoiceId,
            AdvanceRequestId requestId) {

        boolean isRecall() {
            return recall != null;
        }

        LocalDate printDate() {
            return isRecall() ? recall.date() : documentDate;
        }

        DocumentType expectedType() {
            if (isRecall()) {
                return DocumentType.RECALL;
            }
            return kind == PaymentReference.DocumentKind.INVOICE ? DocumentType.INVOICE : DocumentType.ADVANCE_REQUEST;
        }
    }

    private Source invoiceSource(Invoice invoice, Recall recall) {
        List<Customer> recipients = recipients(invoice.folderId(), invoice.date());
        return new Source(PaymentReference.DocumentKind.INVOICE, invoice.id().value(), invoice.folderId(),
                invoice.date(), recall, previousRecall(invoice.recalls(), recall), invoice.terms().delayDays(),
                invoice.dueDate(), recall == null ? "Facture" : "Rappel", recall == null ? "f" : "fr",
                "Facture " + invoice.id().value(), !invoice.distribution().shares().isEmpty(),
                invoice::shareOf, invoice::remainingSumFor, invoice.amount(), invoice.remainingSum(),
                statement(invoice, recipients), invoice.id(), null);
    }

    private Source requestSource(AdvanceRequest request, Recall recall) {
        List<Advance> received = advances.findByRequest(request.id());
        return new Source(PaymentReference.DocumentKind.ADVANCE_REQUEST, request.id().value(), request.folderId(),
                request.date(), recall, previousRecall(request.recalls(), recall), request.terms().delayDays(),
                request.dueDate(), recall == null ? "Demande de provision" : "Rappel", recall == null ? "p" : "pr",
                "Provision " + request.id().value(), !request.distribution().shares().isEmpty(),
                request::shareOf, c -> request.remainingSumFor(c, received), request.amount(),
                request.remainingSum(received), null, null, request.id());
    }

    private PrintedDocument print(Source source, PrintOptions options, Username issuer) {
        DocumentModel model = Ids.require(models.findById(options.modelId()), "Document model", options.modelId().value());
        DocumentType type = model.details().type();
        if (type != DocumentType.UNDEFINED && type != source.expectedType()) {
            throw new DomainException(ErrorCode.MODEL_TYPE_MISMATCH, "Model '" + model.name() + "' is meant for " + type + " documents, not "
                    + source.expectedType())
                            .with("model", model.name())
                            .with("modelType", type)
                            .with("documentType", source.expectedType());
        }
        User user = Ids.require(users.findByUsername(issuer), "User", issuer.value());
        List<Customer> recipients = recipients(source.folderId(), source.documentDate());

        String raw = options.content() != null ? options.content() : model.details().content();
        boolean html = TemplateText.isHtml(raw);
        String content = TemplateText.fill(html ? raw : TemplateText.escape(raw),
                placeholders(source, model, user, recipients, settings.invoicing().vatNumber()));

        List<PrintJob.PageKind> pages = pages(model, source.statement() != null);
        List<PrintJob.Copy> copies = copies(source, recipients, options.mergeAddresses());

        String fileName = source.folderId().value() + "_" + source.filePrefix() + source.documentId() + "_"
                + DateTimeFormatter.BASIC_ISO_DATE.format(source.printDate()) + "_"
                + model.name().replaceAll("[^A-Za-z0-9À-ÿ]+", "_") + ".pdf";
        PrintJob job = new PrintJob(source.subject(), fileName,
                Strings.isBlank(user.shortcut()) ? String.valueOf(source.documentId())
                        : user.shortcut() + "/" + source.documentId(),
                settings.invoicing().vatNumber(), source.printDate(), model.details().title(), content, html, pages,
                source.statement(), Math.max(0, options.attachments()), copies);
        return new PrintedDocument(fileName, renderer.render(job));
    }

    // ----- recipients and copies -------------------------------------------------------------------------------

    private List<Customer> recipients(FolderId folderId, LocalDate date) {
        List<Customer> active = customers.findByFolder(folderId).stream().filter(c -> c.isActiveAt(date)).toList();
        if (active.isEmpty()) {
            throw new DomainException(ErrorCode.NO_ACTIVE_CUSTOMER, "No customer of folder " + folderId.value() + " is active on " + date)
                    .with("folderId", folderId.value())
                    .with("date", date);
        }
        return active.stream().limit(MAX_RECIPIENTS).toList();
    }

    /** What the customer has to pay: everything when he pays for all or is alone, otherwise his share. */
    private static Money dueBy(Customer customer, List<Customer> recipients, Source source, boolean remaining) {
        if (customer.isBilling() || recipients.size() == 1 && !source.hasDistribution()) {
            return remaining ? source.remainingTotal() : source.total();
        }
        if (recipients.size() == 1) {
            return remaining ? source.remainingFor().apply(customer.id()) : source.share().apply(customer.id());
        }
        Customer billing = recipients.stream().filter(Customer::isBilling).findFirst().orElse(null);
        if (billing != null) {
            return Money.ZERO;
        }
        return remaining ? source.remainingFor().apply(customer.id()) : source.share().apply(customer.id());
    }

    private List<PrintJob.Copy> copies(Source source, List<Customer> recipients, boolean merge) {
        if (merge && recipients.size() > 1) {
            List<String> merged = mergedAddress(recipients.get(0), recipients.get(1));
            return List.of(new PrintJob.Copy(List.of(merged),
                    slip(source, recipients.get(0), merged.get(1), source.remainingTotal())));
        }
        List<PrintJob.Copy> copies = new ArrayList<>();
        for (int shift = 0; shift < recipients.size(); shift++) {
            List<Customer> rotated = rotate(recipients, shift);
            Customer recipient = rotated.get(0);
            copies.add(new PrintJob.Copy(rotated.stream().map(DocumentUseCases::address).toList(),
                    slip(source, recipient, recipient.fullName(), dueBy(recipient, recipients, source, true))));
        }
        return copies;
    }

    /** Same order as the legacy application: the last recipient moves to the front at each copy. */
    private static List<Customer> rotate(List<Customer> recipients, int shift) {
        List<Customer> rotated = new ArrayList<>(recipients);
        java.util.Collections.rotate(rotated, shift);
        return rotated;
    }

    private static List<String> address(Customer customer) {
        CustomerDetails d = customer.details();
        PostalAddress a = d.address();
        return lines(d.title(), customer.fullName(), a == null ? null : a.street(),
                a == null ? null : join(" ", a.zip(), a.city()));
    }

    /** "Madame et Monsieur", "Anne et Paul Dupont": the first address for both. */
    private static List<String> mergedAddress(Customer first, Customer second) {
        CustomerDetails a = first.details();
        CustomerDetails b = second.details();
        PostalAddress postal = a.address();
        return lines(join(" et ", a.title(), b.title()),
                join(" ", join(" et ", a.firstName(), b.firstName()), a.lastName()),
                postal == null ? null : postal.street(), postal == null ? null : join(" ", postal.zip(), postal.city()));
    }

    private static List<String> lines(String... lines) {
        return Stream.of(lines).map(l -> l == null ? "" : l.trim()).toList();
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts).filter(p -> !Strings.isBlank(p)).map(String::trim).collect(Collectors.joining(separator));
    }

    // ----- QR-bill ---------------------------------------------------------------------------------------------

    private PrintJob.PaymentSlip slip(Source source, Customer debtor, String debtorName, Money due) {
        PaymentReference.Style style = renderer.paymentReferenceStyle();
        if (style == PaymentReference.Style.NONE || due == null || !due.isPositive()) {
            return null;
        }
        String reference = PaymentReference.create(style,
                PaymentReference.payload(source.kind(), source.documentId(), debtor.id().value()));
        if (tickets.findByNumber(reference).isEmpty()) {
            tickets.save(Ticket.issue(reference, source.folderId(), debtor.id(), source.invoiceId(), source.requestId()));
        }
        PostalAddress a = debtor.details().address();
        return new PrintJob.PaymentSlip(debtorName, a == null ? null : a.street(), a == null ? null : a.zip(),
                a == null ? null : a.city(), due, reference, source.paymentMessage());
    }

    // ----- template --------------------------------------------------------------------------------------------

    /**
     * Placeholders of the legacy templates, plus {@code dueDate}. Amounts are the customers' shares (what they
     * still owe, on reminders).
     */
    private static Map<String, String> placeholders(Source source, DocumentModel model, User user,
                                                    List<Customer> recipients, String vatNumber) {
        Map<String, String> values = new HashMap<>();
        values.put("issuerName", user.fullName());
        values.put("issuerShortcut", user.shortcut());
        values.put("refNo", String.valueOf(source.documentId()));
        values.put("tvaNo", vatNumber);
        values.put("date", longDate(source.printDate()));
        values.put("delay", String.valueOf(source.delayDays()));
        values.put("dueDate", longDate(source.dueDate()));
        values.put("modelTitle", model.details().title());
        values.put("invoiceDate", longDate(source.documentDate()));
        values.put("lastRecallDate", source.previousRecall() == null ? "" : shortDate(source.previousRecall().date()));
        boolean remaining = source.isRecall();
        for (int i = 0; i < MAX_RECIPIENTS; i++) {
            String n = "_" + (i + 1);
            Customer c = i < recipients.size() ? recipients.get(i) : null;
            CustomerDetails d = c == null ? null : c.details();
            PostalAddress a = d == null ? null : d.address();
            values.put("title" + n, d == null ? "" : d.title());
            values.put("firstName" + n, d == null ? "" : d.firstName());
            values.put("lastName" + n, d == null ? "" : d.lastName());
            values.put("streetNo" + n, a == null ? "" : a.street());
            values.put("zip" + n, a == null ? "" : a.zip());
            values.put("city" + n, a == null ? "" : a.city());
            values.put("amount" + n, c == null ? amount(Money.ZERO) : amount(dueBy(c, recipients, source, remaining)));
        }
        Customer billing = recipients.stream().filter(Customer::isBilling).findFirst().orElse(null);
        values.put("amount", billing != null ? amount(dueBy(billing, recipients, source, remaining)) : values.get("amount_1"));
        values.replaceAll((k, v) -> v == null ? "" : v);
        return values;
    }

    /** Pages of the template: legacy layouts named "*_detail.php" are the invoice statement, the others letters. */
    private static List<PrintJob.PageKind> pages(DocumentModel model, boolean statementAvailable) {
        List<PrintJob.PageKind> pages = new ArrayList<>();
        for (PageLayout layout : model.details().layouts()) {
            if (Strings.isBlank(layout.layout())) {
                continue;
            }
            if (layout.layout().contains("detail")) {
                if (statementAvailable) {
                    pages.add(PrintJob.PageKind.INVOICE_STATEMENT);
                }
            } else {
                pages.add(PrintJob.PageKind.LETTER);
            }
        }
        return pages.isEmpty() ? List.of(PrintJob.PageKind.LETTER) : pages;
    }

    // ----- invoice statement -----------------------------------------------------------------------------------

    private PrintJob.InvoiceStatement statement(Invoice invoice, List<Customer> recipients) {
        List<PrintJob.InvoiceStatement.Line> lines = honoraries.findByInvoice(invoice.id()).stream()
                .map(DocumentUseCases::line)
                .toList();
        List<Advance> deducted = advances.findByInvoice(invoice.id());
        List<PrintJob.InvoiceStatement.Balance> balances = new ArrayList<>();
        if (recipients.size() > 1) {
            for (Customer c : recipients) {
                List<Advance> own = deducted.stream().filter(a -> c.id().equals(a.customerId())).toList();
                Money advanced = own.stream().collect(Money.summing(Advance::amount));
                Money due = invoice.shareOf(c.id());
                balances.add(new PrintJob.InvoiceStatement.Balance(join(" ", c.details().title(), c.fullName()),
                        due.plus(advanced), advanceLines(own), advanced, due));
            }
        } else {
            balances.add(new PrintJob.InvoiceStatement.Balance(null, null, advanceLines(deducted),
                    invoice.advancedSum(), invoice.amount()));
        }
        return new PrintJob.InvoiceStatement(lines, invoice.honorariesSum(),
                invoice.discount().isPositive() ? invoice.discount() : null, invoice.taxRate().percent(),
                invoice.tax(), invoice.amount().plus(invoice.advancedSum()), balances, invoice.terms().delayDays());
    }

    private static PrintJob.InvoiceStatement.Line line(Honorary h) {
        return new PrintJob.InvoiceStatement.Line(h.work().date(), h.work().description(), h.work().minutes(),
                h.work().hourlyRate(), h.work().expenses().stream().collect(Money.summing(Expense::amount)), h.amount());
    }

    private static List<PrintJob.InvoiceStatement.AdvanceLine> advanceLines(List<Advance> list) {
        return list.stream()
                .sorted(Comparator.comparing(Advance::date, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(a -> new PrintJob.InvoiceStatement.AdvanceLine(a.date(), a.amount()))
                .toList();
    }

    // ----- loading ---------------------------------------------------------------------------------------------

    private Invoice loadInvoice(InvoiceId id) {
        return Ids.require(invoices.findById(id), "Invoice", id.value());
    }

    private AdvanceRequest loadRequest(AdvanceRequestId id) {
        return Ids.require(requests.findById(id), "Advance request", id.value());
    }

    private static Recall findRecall(List<Recall> recalls, RecallId id) {
        return recalls.stream().filter(r -> id.equals(r.id())).findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.RECALL_NOT_OF_DOCUMENT, "Recall " + id.value() + " does not belong to this document")
                        .with("recallId", id.value()));
    }

    private static Recall previousRecall(List<Recall> recalls, Recall recall) {
        if (recall == null) {
            return null;
        }
        return recalls.stream().filter(r -> r.degree() < recall.degree())
                .max(Comparator.comparingInt(Recall::degree)).orElse(null);
    }
}
