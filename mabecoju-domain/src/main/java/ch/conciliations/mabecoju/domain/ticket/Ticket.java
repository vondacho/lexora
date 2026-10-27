package ch.conciliations.mabecoju.domain.ticket;

import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Strings;

import java.util.Objects;

/**
 * A payment slip reference (BVR / QR reference) issued to a customer of a folder, possibly for a given invoice or
 * advance request. Incoming payments and advances are matched to their debtor through it. The special number
 * {@value #CASH} stands for cash.
 */
public final class Ticket {

    public static final String CASH = "Comptant";

    private final TicketId id;
    private final FolderId folderId;
    private final CustomerId customerId;
    private String number;
    private InvoiceId invoiceId;
    private AdvanceRequestId requestId;

    private Ticket(TicketId id, String number, FolderId folderId, CustomerId customerId, InvoiceId invoiceId,
                   AdvanceRequestId requestId) {
        this.id = id;
        this.number = validate(number);
        this.folderId = folderId;
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.requestId = requestId;
    }

    public static Ticket issue(String number, FolderId folderId, CustomerId customerId) {
        return new Ticket(null, number, folderId, customerId, null, null);
    }

    /**
     * Issues a slip for a document.
     *
     * @param invoiceId invoice the slip is printed on, or null
     * @param requestId advance request the slip is printed on, or null
     */
    public static Ticket issue(String number, FolderId folderId, CustomerId customerId, InvoiceId invoiceId,
                               AdvanceRequestId requestId) {
        return new Ticket(null, number, folderId, customerId, invoiceId, requestId);
    }

    public static Ticket restore(TicketId id, String number, FolderId folderId, CustomerId customerId,
                                 InvoiceId invoiceId, AdvanceRequestId requestId) {
        return new Ticket(Objects.requireNonNull(id, "id"), number, folderId, customerId, invoiceId, requestId);
    }

    public void renumber(String number) {
        this.number = validate(number);
    }

    /** The invoice the slip was issued for no longer exists. */
    public void forgetInvoice() {
        this.invoiceId = null;
    }

    /** The advance request the slip was issued for no longer exists. */
    public void forgetRequest() {
        this.requestId = null;
    }

    private static String validate(String number) {
        Strings.requireNotBlank(number, "Ticket number");
        return Strings.requireMaxLength(number.trim(), 128, "Ticket number");
    }

    /** Several cash tickets may coexist; other numbers identify a single ticket. */
    public boolean isCash() {
        return CASH.equals(number);
    }

    /**
     * Extracts the reference number from a scanned payment slip line ({@code ...>REFERENCE+...}),
     * or returns the input without spaces when it is not a scanned line (legacy {@code ScanningHelper}).
     */
    public static String decodeReference(String scanned) {
        if (scanned == null) {
            return null;
        }
        String stream = scanned.replace(" ", "");
        int begin = stream.indexOf('>');
        int end = stream.indexOf('+');
        if (begin > 0 && end > begin) {
            return stream.substring(begin + 1, end);
        }
        return stream;
    }

    public TicketId id() {
        return id;
    }

    public String number() {
        return number;
    }

    public FolderId folderId() {
        return folderId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public InvoiceId invoiceId() {
        return invoiceId;
    }

    public AdvanceRequestId requestId() {
        return requestId;
    }
}
