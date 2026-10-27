package io.lexora.application.booking;

import io.lexora.application.common.Ids;
import io.lexora.domain.booking.OutgoingPayment;
import io.lexora.domain.booking.OutgoingPaymentDetails;
import io.lexora.domain.booking.OutgoingPaymentId;
import io.lexora.domain.booking.OutgoingPaymentRepository;
import io.lexora.domain.booking.StandingOrder;
import io.lexora.domain.booking.StandingOrderDetails;
import io.lexora.domain.booking.StandingOrderId;
import io.lexora.domain.booking.StandingOrderRepository;
import io.lexora.domain.booking.VatPayment;
import io.lexora.domain.booking.VatPaymentRepository;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
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
