package ch.conciliations.mabecoju.infrastructure.web.invoicing;

import ch.conciliations.mabecoju.application.honorary.HonoraryUseCases;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.honorary.HonoraryId;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.HonorariesApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.HonoraryDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.HonoraryInputDto;
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
