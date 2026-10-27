package io.lexora.domain.booking;

import io.lexora.domain.folder.FolderId;

import java.util.List;
import java.util.Optional;

public interface StandingOrderRepository {

    Optional<StandingOrder> findById(StandingOrderId id);

    /** Orders of the office books (without folder), most recent first. */
    List<StandingOrder> findOfficeOrders();

    List<StandingOrder> findByFolder(FolderId folderId);

    StandingOrder save(StandingOrder order);

    void delete(StandingOrderId id);
}
