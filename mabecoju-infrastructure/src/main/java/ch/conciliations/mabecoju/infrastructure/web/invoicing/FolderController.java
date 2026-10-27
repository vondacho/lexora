package ch.conciliations.mabecoju.infrastructure.web.invoicing;

import ch.conciliations.mabecoju.application.folder.FolderUseCases;
import ch.conciliations.mabecoju.application.folder.OpenFolderCommand;
import ch.conciliations.mabecoju.application.folder.OpenedFolder;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderSearch;
import ch.conciliations.mabecoju.domain.folder.FolderStatus;
import ch.conciliations.mabecoju.domain.folder.FolderType;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.FoldersApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderStatusDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderStatusInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderTypeDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.OpenFolderInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.OpenedFolderDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class FolderController implements FoldersApi {

    private final FolderUseCases folders;

    FolderController(FolderUseCases folders) {
        this.folders = folders;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_SEARCH)
    public List<FolderDto> searchFolders(String name, String number, String customer, FolderTypeDto type) {
        return Json.map(folders.searchFolders(new FolderSearch(name, number, customer,
                Json.sameName(type, FolderType.class))), InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_CREATE)
    public OpenedFolderDto openFolder(OpenFolderInputDto input) {
        OpenedFolder opened = folders.openFolder(new OpenFolderCommand(InvoicingMapper.toDetails(input.getFolder()),
                input.getFirstCustomer() == null ? null : InvoicingMapper.toDetails(input.getFirstCustomer())));
        return new OpenedFolderDto().folder(InvoicingMapper.toDto(opened.folder()))
                .firstCustomer(opened.customer() == null ? null : InvoicingMapper.toDto(opened.customer()));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_DISPLAY)
    public FolderDto getFolder(Long id) {
        return InvoicingMapper.toDto(folders.getFolder(FolderId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_MODIFY)
    public FolderDto updateFolder(Long id, FolderInputDto input) {
        return InvoicingMapper.toDto(folders.updateFolder(FolderId.of(id), InvoicingMapper.toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_MODIFY)
    public void deleteFolder(Long id) {
        folders.deleteFolder(FolderId.of(id));
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public List<FolderStatusDto> listFolderStatuses() {
        return Json.map(folders.listStatuses(), FolderController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_STATUS_MODIFY)
    public FolderStatusDto addFolderStatus(FolderStatusInputDto input) {
        return toDto(folders.addStatus(input.getName()));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_STATUS_MODIFY)
    public void deleteFolderStatus(Long id) {
        folders.deleteStatus(id);
    }

    private static FolderStatusDto toDto(FolderStatus status) {
        return new FolderStatusDto().id(status.id()).name(status.name());
    }
}
