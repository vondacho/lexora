package io.lexora.infrastructure.persistence.booking;

import io.lexora.domain.booking.StandingOrder;
import io.lexora.domain.booking.StandingOrderDetails;
import io.lexora.domain.booking.StandingOrderId;
import io.lexora.domain.booking.StandingOrderRepository;
import io.lexora.domain.folder.FolderId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.isTrue;
import static io.lexora.infrastructure.persistence.support.Conversions.money;
import static io.lexora.infrastructure.persistence.support.Conversions.rate;
import static io.lexora.infrastructure.persistence.support.Conversions.reference;
import static io.lexora.infrastructure.persistence.support.Conversions.toFloat;
import static io.lexora.infrastructure.persistence.support.Conversions.toInt;
import static io.lexora.infrastructure.persistence.support.Conversions.toIntOrNull;

@Repository
class StandingOrderRepositoryAdapter implements StandingOrderRepository {

    private final StandingOrderJpaRepository jpa;

    StandingOrderRepositoryAdapter(StandingOrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<StandingOrder> findById(StandingOrderId id) {
        return jpa.findById(toInt(id.value())).map(StandingOrderRepositoryAdapter::toDomain);
    }

    @Override
    public List<StandingOrder> findOfficeOrders() {
        return jpa.findByFolderIdIsNullOrderByIdDesc().stream().map(StandingOrderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<StandingOrder> findByFolder(FolderId folderId) {
        return jpa.findByFolderIdOrderByIdDesc(toInt(folderId.value())).stream()
                .map(StandingOrderRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public StandingOrder save(StandingOrder order) {
        StandingOrderEntity e = order.id() == null
                ? new StandingOrderEntity()
                : jpa.findById(toInt(order.id().value())).orElseThrow();
        StandingOrderDetails d = order.details();
        e.folderId = toIntOrNull(order.folderId() == null ? null : order.folderId().value());
        e.creditor = d.creditor();
        e.title = d.title();
        e.amount = toFloat(d.amount());
        e.taxRate = toFloat(d.taxRate());
        e.tax = toFloat(order.tax());
        e.day = d.dayOfMonth();
        e.enabled = d.enabled();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(StandingOrderId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static StandingOrder toDomain(StandingOrderEntity e) {
        Long folder = reference(e.folderId);
        int day = e.day == null || e.day < 1 ? 1 : Math.min(e.day, 31);
        return StandingOrder.restore(StandingOrderId.of(e.id), folder == null ? null : FolderId.of(folder),
                new StandingOrderDetails(e.creditor, e.title, money(e.amount), rate(e.taxRate), day, isTrue(e.enabled)),
                money(e.tax));
    }
}
