package ch.conciliations.mabecoju.application.advance;

import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequest;
import ch.conciliations.mabecoju.domain.shared.Money;

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
