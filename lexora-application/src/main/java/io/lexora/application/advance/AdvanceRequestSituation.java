package io.lexora.application.advance;

import io.lexora.domain.advance.Advance;
import io.lexora.domain.advance.AdvanceRequest;
import io.lexora.domain.shared.Money;

import java.util.List;

/** An advance request together with the advances received for it. */
public record AdvanceRequestSituation(AdvanceRequest request, List<Advance> advances) {

    public Money paidSum() {
        return request.paidSum(advances);
    }

    public Money remainingSum() {
        return request.remainingSum(advances);
    }

    public boolean isInvoiced() {
        return request.isInvoiced(advances);
    }
}
