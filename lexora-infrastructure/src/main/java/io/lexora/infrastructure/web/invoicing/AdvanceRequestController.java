package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.advance.AdvanceRequestDraft;
import io.lexora.application.advance.AdvanceRequestUseCases;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.billing.RecallId;
import io.lexora.domain.folder.FolderId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.AdvanceRequestsApi;
import io.lexora.infrastructure.web.api.model.AdvanceRequestDraftDto;
import io.lexora.infrastructure.web.api.model.AdvanceRequestDto;
import io.lexora.infrastructure.web.api.model.AdvanceRequestTermsInputDto;
import io.lexora.infrastructure.web.api.model.DateInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class AdvanceRequestController implements AdvanceRequestsApi {

    private final AdvanceRequestUseCases requests;

    AdvanceRequestController(AdvanceRequestUseCases requests) {
        this.requests = requests;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_REQUESTS)
    public List<AdvanceRequestDto> listFolderAdvanceRequests(Long folderId, Boolean paid, Boolean invoiced) {
        return Json.map(requests.listRequests(FolderId.of(folderId), paid, invoiced), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_CREATE)
    public AdvanceRequestDraftDto prepareAdvanceRequest(Long folderId) {
        AdvanceRequestDraft d = requests.prepareRequest(FolderId.of(folderId));
        return new AdvanceRequestDraftDto().folderId(d.folderId().value())
                .openHonorariesSum(Json.amount(d.openHonorariesSum()))
                .availableAdvancesSum(Json.amount(d.availableAdvancesSum()))
                .uncoveredHonorariesSum(Json.amount(d.uncoveredHonorariesSum()))
                .customers(Json.map(d.customers(), InvoicingMapper::toDto))
                .defaultDelayDays(d.defaultDelayDays());
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_CREATE)
    public AdvanceRequestDto createAdvanceRequest(Long folderId, AdvanceRequestTermsInputDto input) {
        return InvoicingMapper.toDto(requests.createRequest(FolderId.of(folderId), InvoicingMapper.toTerms(input)));
    }

    @Override
    @PreAuthorize(Permissions.DEBTORS_OPEN)
    public List<AdvanceRequestDto> listOpenAdvanceRequests() {
        return Json.map(requests.listOpenRequests(), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_DISPLAY)
    public AdvanceRequestDto getAdvanceRequest(Long id) {
        return InvoicingMapper.toDto(requests.getRequest(AdvanceRequestId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public AdvanceRequestDto reviseAdvanceRequest(Long id, AdvanceRequestTermsInputDto input) {
        return InvoicingMapper.toDto(requests.reviseRequest(AdvanceRequestId.of(id), InvoicingMapper.toTerms(input)));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public void deleteAdvanceRequest(Long id) {
        requests.deleteRequest(AdvanceRequestId.of(id));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public AdvanceRequestDto settleAdvanceRequest(Long id, DateInputDto input) {
        return InvoicingMapper.toDto(requests.settleRequest(AdvanceRequestId.of(id), Json.dateOf(input)));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public AdvanceRequestDto reopenAdvanceRequest(Long id) {
        return InvoicingMapper.toDto(requests.reopenRequest(AdvanceRequestId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public AdvanceRequestDto recallAdvanceRequest(Long id, DateInputDto input) {
        return InvoicingMapper.toDto(requests.recallRequest(AdvanceRequestId.of(id), Json.dateOf(input)));
    }

    @Override
    @PreAuthorize(Permissions.REQUEST_MODIFY)
    public AdvanceRequestDto cancelAdvanceRequestRecall(Long id, Long recallId) {
        return InvoicingMapper.toDto(requests.cancelRecall(AdvanceRequestId.of(id), RecallId.of(recallId)));
    }
}
