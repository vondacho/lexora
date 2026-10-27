package ch.conciliations.mabecoju.infrastructure.persistence.advance;

import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestTerms;
import ch.conciliations.mabecoju.domain.billing.Distribution;
import ch.conciliations.mabecoju.domain.billing.Recall;
import ch.conciliations.mabecoju.domain.billing.RecallId;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toInt;

@Repository
class AdvanceRequestRepositoryAdapter implements AdvanceRequestRepository {

    private final AdvanceRequestJpaRepository jpa;

    AdvanceRequestRepositoryAdapter(AdvanceRequestJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AdvanceRequest> findById(AdvanceRequestId id) {
        return jpa.findById(toInt(id.value())).map(AdvanceRequestRepositoryAdapter::toDomain);
    }

    @Override
    public List<AdvanceRequest> findByFolder(FolderId folderId) {
        return jpa.findByFolderIdOrderByDateDescIdDesc(toInt(folderId.value())).stream()
                .map(AdvanceRequestRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<AdvanceRequest> findOpen() {
        return jpa.findByPaidOnIsNullOrderByDateAscIdAsc().stream()
                .map(AdvanceRequestRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public AdvanceRequest save(AdvanceRequest request) {
        AdvanceRequestEntity e = request.id() == null
                ? new AdvanceRequestEntity()
                : jpa.findById(toInt(request.id().value())).orElseThrow();
        AdvanceRequestTerms terms = request.terms();
        e.folderId = toInt(request.folderId().value());
        e.date = terms.date();
        e.meetingDate = terms.meetingDate();
        e.delay = terms.delayDays();
        e.amount = toFloat(terms.amount());
        e.comment = terms.comment();
        e.honorariesSum = toFloat(request.honorariesSum());
        e.advancedSum = toFloat(request.advancedSum());
        e.modelCode = request.legacyModelCode();
        e.paidOn = request.paidOn();

        e.distribution.clear();
        request.distribution().shares().forEach((customer, share) ->
                e.distribution.add(new AdvanceRequestDistributionEntity(e, toInt(customer.value()), toFloat(share))));

        Set<Integer> kept = request.recalls().stream().map(Recall::id).filter(Objects::nonNull)
                .map(id -> toInt(id.value())).collect(Collectors.toSet());
        e.recalls.removeIf(r -> !kept.contains(r.id));
        request.recalls().stream().filter(r -> r.id() == null).forEach(r -> {
            AdvanceRequestRecallEntity recall = new AdvanceRequestRecallEntity();
            recall.request = e;
            recall.date = r.date();
            recall.degree = r.degree();
            recall.amount = toFloat(r.amount());
            e.recalls.add(recall);
        });
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(AdvanceRequestId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static AdvanceRequest toDomain(AdvanceRequestEntity e) {
        Map<CustomerId, Money> shares = new LinkedHashMap<>();
        e.distribution.forEach(d -> shares.put(CustomerId.of(d.customerId), money(d.amount)));
        AdvanceRequestTerms terms = new AdvanceRequestTerms(e.date, e.meetingDate, e.delay == null ? 0 : e.delay,
                money(e.amount), new Distribution(shares), e.comment);
        List<Recall> recalls = e.recalls.stream()
                .map(r -> new Recall(RecallId.of(r.id), r.date, r.degree == null ? 0 : r.degree, money(r.amount)))
                .toList();
        return AdvanceRequest.restore(AdvanceRequestId.of(e.id), FolderId.of(e.folderId), money(e.honorariesSum),
                money(e.advancedSum), e.modelCode, terms, e.paidOn, recalls);
    }
}
