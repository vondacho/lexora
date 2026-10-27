package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.util.List;

/** Everything still to be cashed in (legacy "débiteurs ouverts"). */
public record DebtorsOverview(
        List<FolderToInvoice> foldersToInvoice,
        List<OpenDebt> openInvoices,
        List<OpenDebt> openRequests) {

    /** Sum over folders of the honoraries not covered by advances. */
    public Money foldersTotal() {
        return foldersToInvoice.stream().collect(Money.summing(FolderToInvoice::uncovered));
    }

    public Money invoicesTotal() {
        return openInvoices.stream().collect(Money.summing(OpenDebt::remaining));
    }

    public Money requestsTotal() {
        return openRequests.stream().collect(Money.summing(OpenDebt::remaining));
    }

    /**
     * @param toBeInvoiced open honoraries
     * @param available    advances not yet deducted
     */
    public record FolderToInvoice(FolderId folderId, String folderName, Money toBeInvoiced, Money available) {

        public Money uncovered() {
            return toBeInvoiced.minus(available).atLeastZero();
        }
    }

    /**
     * @param documentId  invoice or advance request id
     * @param daysOverdue negative while not yet due
     */
    public record OpenDebt(
            long documentId,
            FolderId folderId,
            String folderName,
            LocalDate date,
            LocalDate dueDate,
            long daysOverdue,
            Money amount,
            Money remaining,
            int recallCount,
            LocalDate lastRecallDate) {
    }
}
