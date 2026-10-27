package ch.conciliations.mabecoju.application.advance;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceId;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.util.List;

/** Receiving provisions, with or without a prior request. */
@Transactional
public class AdvanceUseCases {

    private final AdvanceRepository advances;
    private final AdvanceRequestRepository requests;
    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final TicketRepository tickets;

    public AdvanceUseCases(AdvanceRepository advances, AdvanceRequestRepository requests,
                           CustomerRepository customers, FolderRepository folders, TicketRepository tickets) {
        this.advances = advances;
        this.requests = requests;
        this.customers = customers;
        this.folders = folders;
        this.tickets = tickets;
    }

    /** Records an advance; the request it answers is settled once fully covered. */
    public Advance receiveAdvance(ReceiveAdvanceCommand command) {
        FolderId folderId = command.folderId();
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        if (command.customerId() != null) {
            Customer customer = Ids.require(customers.findById(command.customerId()), "Customer", command.customerId().value());
            if (!customer.folderId().equals(folderId)) {
                throw new DomainException(ErrorCode.NOT_A_PARTY, "Customer " + customer.id().value() + " is not a party of folder " + folderId.value())
                        .with("customerId", customer.id().value())
                        .with("folderId", folderId.value());
            }
        }
        if (command.means() != null && command.means().ticketId() != null) {
            Ids.require(tickets.findById(command.means().ticketId()), "Ticket", command.means().ticketId().value());
        }
        AdvanceRequest request = null;
        if (command.requestId() != null) {
            request = Ids.require(requests.findById(command.requestId()), "Advance request", command.requestId().value());
            if (!request.folderId().equals(folderId)) {
                throw new DomainException(ErrorCode.OTHER_FOLDER, "Advance request " + request.id().value() + " belongs to another folder")
                        .with("document", "P-" + request.id().value());
            }
        }
        Advance advance = advances.save(Advance.receive(folderId, command.requestId(), command.customerId(),
                command.date(), command.amount(), command.means()));
        if (request != null) {
            request.settleIfCovered(advances.findByRequest(request.id()));
            requests.save(request);
        }
        return advance;
    }

    /**
     * Cancels an advance that has not been deducted on an invoice yet; the request it answered is reopened if no
     * longer covered.
     */
    public void cancelAdvance(AdvanceId id) {
        Advance advance = getAdvance(id);
        if (advance.isInvoiced()) {
            throw new DomainException(ErrorCode.ADVANCE_DEDUCTED, "Advance " + id.value() + " is deducted on invoice "
                    + advance.invoiceId().value() + "; delete or revise the invoice first")
                            .with("advanceId", id.value())
                            .with("invoiceId", advance.invoiceId().value());
        }
        advances.delete(id);
        if (advance.requestId() != null) {
            requests.findById(advance.requestId()).ifPresent(request -> {
                request.reopenIfUncovered(advances.findByRequest(request.id()));
                requests.save(request);
            });
        }
    }

    public Advance getAdvance(AdvanceId id) {
        return Ids.require(advances.findById(id), "Advance", id.value());
    }

    /**
     * @param invoiced null for all, true for advances already deducted on an invoice, false for available ones
     */
    public List<Advance> listAdvances(FolderId folderId, Boolean invoiced) {
        List<Advance> all = advances.findByFolder(folderId);
        if (invoiced == null) {
            return all;
        }
        return all.stream().filter(a -> a.isInvoiced() == invoiced).toList();
    }
}
