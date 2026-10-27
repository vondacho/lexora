package ch.conciliations.mabecoju.application.advance;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestTerms;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.honorary.Honorary;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.settings.SettingsRepository;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Requesting provisions from the customers of a folder and following their payment. */
@Transactional
public class AdvanceRequestUseCases {

    private final AdvanceRequestRepository requests;
    private final AdvanceRepository advances;
    private final HonoraryRepository honoraries;
    private final CustomerRepository customers;
    private final FolderRepository folders;
    private final SettingsRepository settings;
    private final TicketRepository tickets;
    private final Clock clock;

    public AdvanceRequestUseCases(AdvanceRequestRepository requests, AdvanceRepository advances,
                                  HonoraryRepository honoraries, CustomerRepository customers,
                                  FolderRepository folders, SettingsRepository settings, TicketRepository tickets,
                                  Clock clock) {
        this.requests = requests;
        this.advances = advances;
        this.honoraries = honoraries;
        this.customers = customers;
        this.folders = folders;
        this.settings = settings;
        this.tickets = tickets;
        this.clock = clock;
    }

    public AdvanceRequestDraft prepareRequest(FolderId folderId) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        Money open = openHonorariesSum(folderId);
        Money available = availableAdvancesSum(folderId);
        LocalDate today = LocalDate.now(clock);
        List<Customer> active = customers.findByFolder(folderId).stream().filter(c -> c.isActiveAt(today)).toList();
        return new AdvanceRequestDraft(folderId, open, available, open.minus(available), active,
                settings.invoicing().requestDelayDays());
    }

    public AdvanceRequestSituation createRequest(FolderId folderId, AdvanceRequestTerms terms) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        Money available = availableAdvancesSum(folderId);
        Money uncovered = openHonorariesSum(folderId).minus(available);
        AdvanceRequest request = requests.save(AdvanceRequest.create(folderId, uncovered, available, terms));
        return new AdvanceRequestSituation(request, List.of());
    }

    public AdvanceRequestSituation reviseRequest(AdvanceRequestId id, AdvanceRequestTerms terms) {
        AdvanceRequest request = load(id);
        request.revise(terms);
        return situationOf(requests.save(request));
    }

    /**
     * Deletes the request together with its recalls and the advances received for it (legacy behaviour); the slips
     * issued for it are kept without the link.
     */
    public void deleteRequest(AdvanceRequestId id) {
        load(id);
        tickets.findByRequest(id).forEach(t -> {
            t.forgetRequest();
            tickets.save(t);
        });
        advances.findByRequest(id).forEach(a -> advances.delete(a.id()));
        requests.delete(id);
    }

    public AdvanceRequestSituation getRequest(AdvanceRequestId id) {
        return situationOf(load(id));
    }

    /**
     * @param paid     null for all, otherwise only the paid (true) or open (false) requests
     * @param invoiced null for all, otherwise only the requests whose advances were all invoiced (true) or not
     */
    public List<AdvanceRequestSituation> listRequests(FolderId folderId, Boolean paid, Boolean invoiced) {
        return requests.findByFolder(folderId).stream()
                .map(this::situationOf)
                .filter(s -> paid == null || s.request().isPaid() == paid)
                .filter(s -> invoiced == null || s.isInvoiced() == invoiced)
                .toList();
    }

    public List<AdvanceRequestSituation> listOpenRequests() {
        return requests.findOpen().stream().map(this::situationOf).toList();
    }

    public AdvanceRequestSituation settleRequest(AdvanceRequestId id, LocalDate on) {
        AdvanceRequest request = load(id);
        request.settleOn(on != null ? on : LocalDate.now(clock));
        return situationOf(requests.save(request));
    }

    public AdvanceRequestSituation reopenRequest(AdvanceRequestId id) {
        AdvanceRequest request = load(id);
        request.reopen();
        return situationOf(requests.save(request));
    }

    public AdvanceRequestSituation recallRequest(AdvanceRequestId id, LocalDate on) {
        AdvanceRequest request = load(id);
        request.recall(on != null ? on : LocalDate.now(clock), advances.findByRequest(id));
        return situationOf(requests.save(request));
    }

    public AdvanceRequestSituation cancelRecall(AdvanceRequestId id, RecallId recallId) {
        AdvanceRequest request = load(id);
        request.cancelRecall(recallId);
        return situationOf(requests.save(request));
    }

    private AdvanceRequest load(AdvanceRequestId id) {
        return Ids.require(requests.findById(id), "Advance request", id.value());
    }

    private AdvanceRequestSituation situationOf(AdvanceRequest request) {
        return new AdvanceRequestSituation(request, advances.findByRequest(request.id()));
    }

    private Money openHonorariesSum(FolderId folderId) {
        return honoraries.findNotInvoicedByFolder(folderId).stream().collect(Money.summing(Honorary::amount));
    }

    private Money availableAdvancesSum(FolderId folderId) {
        return advances.findNotInvoicedByFolder(folderId).stream().collect(Money.summing(Advance::amount));
    }
}
