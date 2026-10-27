package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.advance.AdvanceUseCases;
import io.lexora.application.advance.ReceiveAdvanceCommand;
import io.lexora.domain.advance.AdvanceId;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.AdvancesApi;
import io.lexora.infrastructure.web.api.model.AdvanceDto;
import io.lexora.infrastructure.web.api.model.AdvanceInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class AdvanceController implements AdvancesApi {

    private final AdvanceUseCases advances;

    AdvanceController(AdvanceUseCases advances) {
        this.advances = advances;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_CONTENT)
    public List<AdvanceDto> listAdvances(Long folderId, Boolean invoiced) {
        return Json.map(advances.listAdvances(FolderId.of(folderId), invoiced), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.ADVANCE_RECEIPT)
    public AdvanceDto receiveAdvance(Long folderId, AdvanceInputDto input) {
        return InvoicingMapper.toDto(advances.receiveAdvance(new ReceiveAdvanceCommand(FolderId.of(folderId),
                input.getRequestId() == null ? null : AdvanceRequestId.of(input.getRequestId()),
                input.getCustomerId() == null ? null : CustomerId.of(input.getCustomerId()),
                input.getDate(), Json.money(input.getAmount()),
                Json.means(input.getTicketId(), input.getIban(), input.getCash()))));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_CONTENT)
    public AdvanceDto getAdvance(Long id) {
        return InvoicingMapper.toDto(advances.getAdvance(AdvanceId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.ADVANCE_RECEIPT)
    public void cancelAdvance(Long id) {
        advances.cancelAdvance(AdvanceId.of(id));
    }
}
