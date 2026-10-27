package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.honorary.HonoraryUseCases;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.honorary.HonoraryId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.HonorariesApi;
import io.lexora.infrastructure.web.api.model.HonoraryDto;
import io.lexora.infrastructure.web.api.model.HonoraryInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class HonoraryController implements HonorariesApi {

    private final HonoraryUseCases honoraries;

    HonoraryController(HonoraryUseCases honoraries) {
        this.honoraries = honoraries;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_HONORARIES)
    public List<HonoraryDto> listHonoraries(Long folderId, Boolean invoiced) {
        return Json.map(honoraries.listHonoraries(FolderId.of(folderId), invoiced), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.HONORARY_CREATE)
    public HonoraryDto recordHonorary(Long folderId, HonoraryInputDto input) {
        return InvoicingMapper.toDto(honoraries.recordHonorary(FolderId.of(folderId), InvoicingMapper.toWork(input)));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_HONORARIES)
    public HonoraryDto getHonorary(Long id) {
        return InvoicingMapper.toDto(honoraries.getHonorary(HonoraryId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.HONORARY_MODIFY)
    public HonoraryDto reviseHonorary(Long id, HonoraryInputDto input) {
        return InvoicingMapper.toDto(honoraries.reviseHonorary(HonoraryId.of(id), InvoicingMapper.toWork(input)));
    }

    @Override
    @PreAuthorize(Permissions.HONORARY_MODIFY)
    public void deleteHonorary(Long id) {
        honoraries.deleteHonorary(HonoraryId.of(id));
    }
}
