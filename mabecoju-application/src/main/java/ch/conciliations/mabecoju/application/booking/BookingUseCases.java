package ch.conciliations.mabecoju.application.booking;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.booking.OutgoingPayment;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentDetails;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentId;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentRepository;
import ch.conciliations.mabecoju.domain.booking.StandingOrder;
import ch.conciliations.mabecoju.domain.booking.StandingOrderDetails;
import ch.conciliations.mabecoju.domain.booking.StandingOrderId;
import ch.conciliations.mabecoju.domain.booking.StandingOrderRepository;
import ch.conciliations.mabecoju.domain.booking.VatPayment;
import ch.conciliations.mabecoju.domain.booking.VatPaymentRepository;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keeping the books of the office (entries without folder) and of tutelage folders: outgoing payments and
 * standing orders.
 */
@Transactional
public class BookingUseCases {

    private final OutgoingPaymentRepository payments;
    private final StandingOrderRepository orders;
    private final FolderRepository folders;
    private final VatPaymentRepository vatPayments;
    private final Clock clock;

    public BookingUseCases(OutgoingPaymentRepository payments, StandingOrderRepository orders,
                           FolderRepository folders, VatPaymentRepository vatPayments, Clock clock) {
        this.payments = payments;
        this.orders = orders;
        this.folders = folders;
        this.vatPayments = vatPayments;
        this.clock = clock;
    }

    // ----- outgoing payments -----------------------------------------------------------------------------------

    /** @param folderId null for the office books */
    public OutgoingPayment recordPayment(FolderId folderId, OutgoingPaymentDetails details) {
        requireFolderIfAny(folderId);
        return payments.save(OutgoingPayment.recordManual(folderId, details));
    }

    public OutgoingPayment updatePayment(OutgoingPaymentId id, OutgoingPaymentDetails details) {
        OutgoingPayment payment = getPayment(id);
        requireNotVat(id);
        payment.update(details);
        return payments.save(payment);
    }

    public void deletePayment(OutgoingPaymentId id) {
        getPayment(id);
        requireNotVat(id);
        payments.delete(id);
    }

    /** The VAT payments behind office expenses, by expense: those expenses follow their VAT payment. */
    public Map<OutgoingPaymentId, VatPayment> vatPaymentsByExpense() {
        return vatPayments.findAll().stream()
                .filter(v -> v.expenseId() != null)
                .collect(Collectors.toMap(VatPayment::expenseId, v -> v));
    }

    /** An expense booked for a VAT payment changes only through it. */
    private void requireNotVat(OutgoingPaymentId id) {
        vatPayments.findByExpense(id).ifPresent(v -> {
            throw new DomainException(ErrorCode.EXPENSE_FOLLOWS_VAT_PAYMENT, "Payment " + id.value() + " records the VAT paid for " + v.period().from()
                    + " to " + v.period().to() + "; change or delete the VAT payment instead")
                            .with("paymentId", id.value())
                            .with("from", v.period().from())
                            .with("to", v.period().to());
        });
    }

    public OutgoingPayment getPayment(OutgoingPaymentId id) {
        return Ids.require(payments.findById(id), "Payment", id.value());
    }

    /** @param folderId null for the office books */
    public List<OutgoingPayment> listPayments(FolderId folderId) {
        return folderId == null ? payments.findOfficePayments() : payments.findByFolder(folderId);
    }

    // ----- standing orders -------------------------------------------------------------------------------------

    public StandingOrder createOrder(FolderId folderId, StandingOrderDetails details) {
        requireFolderIfAny(folderId);
        return orders.save(StandingOrder.create(folderId, details));
    }

    public StandingOrder updateOrder(StandingOrderId id, StandingOrderDetails details) {
        StandingOrder order = getOrder(id);
        order.update(details);
        return orders.save(order);
    }

    public void deleteOrder(StandingOrderId id) {
        getOrder(id);
        orders.delete(id);
    }

    public StandingOrder getOrder(StandingOrderId id) {
        return Ids.require(orders.findById(id), "Standing order", id.value());
    }

    /** @param folderId null for the office books */
    public List<StandingOrder> listOrders(FolderId folderId) {
        return folderId == null ? orders.findOfficeOrders() : orders.findByFolder(folderId);
    }

    /** Month the standing orders should be executed for next. */
    public YearMonth nextExecutionMonth(FolderId folderId) {
        return StandingOrder.nextExecutionMonth(payments.lastPermanentPaymentDate(folderId), YearMonth.now(clock));
    }

    /**
     * Books the payments of every enabled standing order for the given month (the next one when null).
     *
     * @param folderId null for the office books
     */
    public List<OutgoingPayment> executeOrders(FolderId folderId, YearMonth month) {
        requireFolderIfAny(folderId);
        YearMonth period = month != null ? month : nextExecutionMonth(folderId);
        return listOrders(folderId).stream()
                .map(order -> order.executeFor(period))
                .flatMap(Optional::stream)
                .map(payments::save)
                .toList();
    }

    private void requireFolderIfAny(FolderId folderId) {
        if (folderId != null) {
            Ids.require(folders.findById(folderId), "Folder", folderId.value());
        }
    }
}
