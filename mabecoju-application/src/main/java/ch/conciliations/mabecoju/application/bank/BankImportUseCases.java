package ch.conciliations.mabecoju.application.bank;

import ch.conciliations.mabecoju.application.bank.BankEntryMatch.Problem;
import ch.conciliations.mabecoju.application.bank.BankEntryMatch.Status;
import ch.conciliations.mabecoju.application.bank.BankEntryMatch.Target;
import ch.conciliations.mabecoju.application.common.NotFoundException;
import ch.conciliations.mabecoju.application.debtor.DebtorUseCases;
import ch.conciliations.mabecoju.application.debtor.ReceivePaymentsCommand;
import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.billing.PaymentMeans;
import ch.conciliations.mabecoju.domain.billing.PaymentReference;
import ch.conciliations.mabecoju.domain.billing.PaymentReference.DocumentKind;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.Folder;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.invoice.Invoice;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.ticket.Ticket;
import ch.conciliations.mabecoju.domain.ticket.TicketId;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Import of the bank notifications (camt.054): each credit is matched to the invoice or advance request it pays,
 * by its payment reference, or suggested by amount and payer name; the confirmed ones are recorded as receipts.
 */
@Transactional
public class BankImportUseCases {

    /** A document is settled within 1 CHF (as for the other receipts): such a gap is not an overpayment. */
    private static final Money TOLERANCE = Money.of("1.00");

    private final BankStatementReader reader;
    private final DebtorUseCases debtors;
    private final InvoiceRepository invoices;
    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final TicketRepository tickets;
    private final ImportedBankEntries imported;

    public BankImportUseCases(BankStatementReader reader, DebtorUseCases debtors, InvoiceRepository invoices,
                              AdvanceRequestRepository requests, AdvanceRepository advances,
                              CustomerRepository customers, FolderRepository folders, TicketRepository tickets,
                              ImportedBankEntries imported) {
        this.reader = reader;
        this.debtors = debtors;
        this.invoices = invoices;
        this.requests = requests;
        this.advances = advances;
        this.customers = customers;
        this.folders = folders;
        this.tickets = tickets;
        this.imported = imported;
    }

    public record Analysis(BankStatement statement, List<BankEntryMatch> entries) {
    }

    /** Reads the file and tells, for each credit, what it pays. Nothing is recorded. */
    public Analysis analyse(String content) {
        BankStatement statement = reader.read(content);
        List<Candidate> candidates = openShares();
        List<BankEntryMatch> entries = statement.credits().stream().map(c -> match(c, candidates)).toList();
        return new Analysis(statement, entries);
    }

    /** Records the confirmed credits; a payment already in the books is skipped. */
    public List<BankReceiptResult> record(List<BankReceipt> receipts) {
        List<BankReceiptResult> results = new ArrayList<>();
        for (BankReceipt receipt : receipts) {
            results.add(record(receipt));
        }
        return results;
    }

    // ----- matching --------------------------------------------------------------------------------------------

    private BankEntryMatch match(BankCredit credit, List<Candidate> candidates) {
        if (!"CHF".equalsIgnoreCase(credit.currency())) {
            return new BankEntryMatch(credit, Status.UNMATCHED, Problem.CURRENCY, null, null);
        }
        Optional<Target> byReference = targetOf(credit.reference());
        if (imported.contains(credit.entryReference())) {
            return new BankEntryMatch(credit, Status.DUPLICATE, Problem.NONE, byReference.orElse(null), null);
        }
        if (byReference.isPresent()) {
            Target target = byReference.get();
            if (credit.entryReference() == null && alreadyRecorded(target, credit.date(), credit.amount())) {
                return new BankEntryMatch(credit, Status.DUPLICATE, Problem.NONE, target, null);
            }
            Money remaining = remainingFor(target);
            if (!remaining.isPositive()) {
                return new BankEntryMatch(credit, Status.UNMATCHED, Problem.DOCUMENT_PAID, target, remaining);
            }
            boolean overpaid = credit.amount().compareTo(remaining.plus(TOLERANCE)) > 0;
            return new BankEntryMatch(credit, overpaid ? Status.CHECK : Status.READY,
                    overpaid ? Problem.OVERPAID : Problem.NONE, target, remaining);
        }
        Problem why = credit.reference() == null || credit.reference().isBlank() ? Problem.NOT_FOUND : Problem.UNKNOWN_REFERENCE;
        Optional<Candidate> suggestion = suggest(credit, candidates);
        if (suggestion.isPresent()) {
            Candidate c = suggestion.get();
            if (credit.entryReference() == null && alreadyRecorded(c.target(), credit.date(), credit.amount())) {
                return new BankEntryMatch(credit, Status.DUPLICATE, Problem.NONE, c.target(), null);
            }
            return new BankEntryMatch(credit, Status.CHECK, Problem.SUGGESTED, c.target(), c.remaining());
        }
        return new BankEntryMatch(credit, Status.UNMATCHED, why, null, null);
    }

    /** The document and customer of a reference: its payment slip when registered, else the reference itself. */
    private Optional<Target> targetOf(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String number = Ticket.decodeReference(reference);
        Optional<Ticket> ticket = tickets.findByNumber(number);
        if (ticket.isPresent() && ticket.get().customerId() != null) {
            Ticket t = ticket.get();
            if (t.invoiceId() != null) {
                return target(DocumentKind.INVOICE, t.invoiceId().value(), t.customerId().value());
            }
            if (t.requestId() != null) {
                return target(DocumentKind.ADVANCE_REQUEST, t.requestId().value(), t.customerId().value());
            }
        }
        return PaymentReference.decode(number).flatMap(d -> target(d.kind(), d.documentId(), d.customerId()));
    }

    private Optional<Target> target(DocumentKind kind, long documentId, long customerId) {
        Optional<FolderId> folderId = kind == DocumentKind.INVOICE
                ? invoices.findById(InvoiceId.of(documentId)).map(Invoice::folderId)
                : requests.findById(AdvanceRequestId.of(documentId)).map(AdvanceRequest::folderId);
        Optional<Customer> customer = customers.findById(CustomerId.of(customerId));
        if (folderId.isEmpty() || customer.isEmpty() || !customer.get().folderId().equals(folderId.get())) {
            return Optional.empty();
        }
        String folderName = folders.findById(folderId.get()).map(Folder::name).orElse(null);
        return Optional.of(new Target(kind, documentId, folderId.get().value(), folderName, customerId,
                customer.get().fullName()));
    }

    /** What the customer still owes on the document. */
    private Money remainingFor(Target target) {
        CustomerId customer = CustomerId.of(target.customerId());
        if (target.kind() == DocumentKind.INVOICE) {
            Invoice invoice = invoices.findById(InvoiceId.of(target.documentId())).orElseThrow();
            return invoice.isPaid() ? Money.ZERO : invoice.remainingSumFor(customer).atLeastZero();
        }
        AdvanceRequest request = requests.findById(AdvanceRequestId.of(target.documentId())).orElseThrow();
        return request.isPaid() ? Money.ZERO
                : request.remainingSumFor(customer, advances.findByRequest(request.id())).atLeastZero();
    }

    /** Same customer, date and amount already on the document: for credits the bank gave no reference to. */
    private boolean alreadyRecorded(Target target, LocalDate date, Money amount) {
        CustomerId customer = CustomerId.of(target.customerId());
        if (target.kind() == DocumentKind.INVOICE) {
            return invoices.findById(InvoiceId.of(target.documentId())).stream()
                    .flatMap(i -> i.payments().stream())
                    .anyMatch(p -> customer.equals(p.customerId()) && date.equals(p.date()) && amount.equals(p.amount()));
        }
        return advances.findByRequest(AdvanceRequestId.of(target.documentId())).stream()
                .anyMatch(a -> customer.equals(a.customerId()) && date.equals(a.date()) && amount.equals(a.amount()));
    }

    private record Candidate(Target target, Money remaining, String lastName) {
    }

    /** Every customer share still owed on an open invoice or request. */
    private List<Candidate> openShares() {
        List<Candidate> list = new ArrayList<>();
        for (Invoice invoice : invoices.findOpen()) {
            for (CustomerId c : invoice.distribution().shares().keySet()) {
                Money remaining = invoice.remainingSumFor(c);
                if (remaining.isPositive()) {
                    target(DocumentKind.INVOICE, invoice.id().value(), c.value())
                            .ifPresent(t -> list.add(new Candidate(t, remaining, lastNameOf(c))));
                }
            }
        }
        for (AdvanceRequest request : requests.findOpen()) {
            List<Advance> received = advances.findByRequest(request.id());
            for (CustomerId c : request.distribution().shares().keySet()) {
                Money remaining = request.remainingSumFor(c, received);
                if (remaining.isPositive()) {
                    target(DocumentKind.ADVANCE_REQUEST, request.id().value(), c.value())
                            .ifPresent(t -> list.add(new Candidate(t, remaining, lastNameOf(c))));
                }
            }
        }
        return list;
    }

    private String lastNameOf(CustomerId id) {
        return customers.findById(id).map(c -> c.details().lastName()).orElse(null);
    }

    /** A share owed for exactly the amount: the one whose name the payer bears, or the only one. */
    private static Optional<Candidate> suggest(BankCredit credit, List<Candidate> candidates) {
        List<Candidate> sameAmount = candidates.stream().filter(c -> c.remaining().equals(credit.amount())).toList();
        String payer = plain(credit.debtorName());
        List<Candidate> named = sameAmount.stream()
                .filter(c -> c.lastName() != null && !payer.isEmpty() && payer.contains(plain(c.lastName())))
                .toList();
        if (named.size() == 1) {
            return Optional.of(named.getFirst());
        }
        return sameAmount.size() == 1 && named.isEmpty() ? Optional.of(sameAmount.getFirst()) : Optional.empty();
    }

    private static String plain(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    // ----- recording -------------------------------------------------------------------------------------------

    private BankReceiptResult record(BankReceipt receipt) {
        if (receipt.amount() == null || !receipt.amount().isPositive() || receipt.date() == null) {
            throw new DomainException(ErrorCode.BANK_RECEIPT_INVALID, "A bank receipt needs a date and a positive amount");
        }
        boolean known = receipt.entryReference() != null && !receipt.entryReference().isBlank();
        if (known && imported.contains(receipt.entryReference())) {
            return new BankReceiptResult(false, false, Money.ZERO);
        }
        Target target = resolve(receipt);
        if (!known && alreadyRecorded(target, receipt.date(), receipt.amount())) {
            return new BankReceiptResult(false, false, Money.ZERO);
        }
        Money remaining = remainingFor(target);
        // within the tolerance the whole credit goes on the document, which it settles
        Money onDocument = receipt.amount().compareTo(remaining.plus(TOLERANCE)) <= 0 ? receipt.amount() : remaining;
        Money surplus = receipt.amount().minus(onDocument);
        FolderId folder = FolderId.of(target.folderId());
        CustomerId customer = CustomerId.of(target.customerId());
        TicketId ticket = receipt.reference() == null ? null
                : tickets.findByNumber(Ticket.decodeReference(receipt.reference())).map(Ticket::id).orElse(null);
        if (onDocument.isPositive()) {
            debtors.receivePayments(new ReceivePaymentsCommand(folder, customer, ticket,
                    target.kind() == DocumentKind.INVOICE
                            ? List.of(new ReceivePaymentsCommand.InvoiceLine(InvoiceId.of(target.documentId()),
                            receipt.date(), onDocument, receipt.iban(), false))
                            : List.of(),
                    target.kind() == DocumentKind.ADVANCE_REQUEST
                            ? List.of(new ReceivePaymentsCommand.RequestLine(AdvanceRequestId.of(target.documentId()),
                            receipt.date(), onDocument, receipt.iban(), false))
                            : List.of()));
        }
        if (surplus.isPositive()) {
            advances.save(Advance.receive(folder, null, customer, receipt.date(), surplus,
                    new PaymentMeans(ticket, receipt.iban(), false)));
        }
        if (known) {
            imported.add(receipt.entryReference(), receipt.date(), receipt.amount(), receipt.iban());
        }
        boolean settled = target.kind() == DocumentKind.INVOICE
                ? invoices.findById(InvoiceId.of(target.documentId())).map(Invoice::isPaid).orElse(false)
                : requests.findById(AdvanceRequestId.of(target.documentId())).map(AdvanceRequest::isPaid).orElse(false);
        return new BankReceiptResult(true, settled, surplus);
    }

    /** The document and payer of a receipt; without payer, the customer owing that amount, else the first owing. */
    private Target resolve(BankReceipt receipt) {
        if (receipt.customerId() != null) {
            return target(receipt.kind(), receipt.documentId(), receipt.customerId())
                    .orElseThrow(() -> new DomainException(ErrorCode.NOT_A_PARTY, "Customer " + receipt.customerId()
                            + " is not a party of " + receipt.kind() + " " + receipt.documentId())
                                    .with("customerId", receipt.customerId())
                                    .with("document", (receipt.kind() == DocumentKind.INVOICE ? "F-" : "P-") + receipt.documentId()));
        }
        List<Candidate> owing = openShares().stream()
                .filter(c -> c.target().kind() == receipt.kind() && c.target().documentId() == receipt.documentId())
                .toList();
        return owing.stream().filter(c -> c.remaining().equals(receipt.amount())).findFirst()
                .or(() -> owing.stream().findFirst())
                .map(Candidate::target)
                .orElseThrow(() -> new NotFoundException(receipt.kind() == DocumentKind.INVOICE ? "Open invoice" : "Open advance request",
                        receipt.documentId()));
    }
}
