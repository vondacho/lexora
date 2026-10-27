package ch.conciliations.mabecoju.domain.booking;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Period;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OutgoingPaymentRepository {

    Optional<OutgoingPayment> findById(OutgoingPaymentId id);

    /** Payments of the office books (without folder), most recent first. */
    List<OutgoingPayment> findOfficePayments();

    /** Payments booked for a folder, most recently paid first. */
    List<OutgoingPayment> findByFolder(FolderId folderId);

    /** Office payments paid within the period. */
    List<OutgoingPayment> findOfficePaidBetween(Period period);

    List<OutgoingPayment> findOfficePaid();

    /** Date of the last standing order execution, in the office books or in a folder. */
    Optional<LocalDate> lastPermanentPaymentDate(FolderId folderIdOrNull);

    OutgoingPayment save(OutgoingPayment payment);

    void delete(OutgoingPaymentId id);
}
