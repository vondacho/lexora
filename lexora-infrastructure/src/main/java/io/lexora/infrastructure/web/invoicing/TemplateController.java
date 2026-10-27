package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.template.TemplateUseCases;
import io.lexora.domain.template.DocumentModel;
import io.lexora.domain.template.DocumentModelDetails;
import io.lexora.domain.template.DocumentModelId;
import io.lexora.domain.template.DocumentType;
import io.lexora.domain.template.PageLayout;
import io.lexora.domain.template.Wording;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.TemplatesApi;
import io.lexora.infrastructure.web.api.model.DocumentModelDto;
import io.lexora.infrastructure.web.api.model.DocumentModelInputDto;
import io.lexora.infrastructure.web.api.model.DocumentTypeDto;
import io.lexora.infrastructure.web.api.model.PageLayoutDto;
import io.lexora.infrastructure.web.api.model.WordingDto;
import io.lexora.infrastructure.web.api.model.WordingInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class TemplateController implements TemplatesApi {

    private final TemplateUseCases templates;

    TemplateController(TemplateUseCases templates) {
        this.templates = templates;
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public List<DocumentModelDto> listDocumentModels(DocumentTypeDto type) {
        return Json.map(templates.listModels(Json.sameName(type, DocumentType.class)), TemplateController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.MODELS)
    public DocumentModelDto createDocumentModel(DocumentModelInputDto input) {
        return toDto(templates.createModel(toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public DocumentModelDto getDocumentModel(Long id) {
        return toDto(templates.getModel(DocumentModelId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.MODELS)
    public DocumentModelDto updateDocumentModel(Long id, DocumentModelInputDto input) {
        return toDto(templates.updateModel(DocumentModelId.of(id), toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.MODELS)
    public void deleteDocumentModel(Long id) {
        templates.deleteModel(DocumentModelId.of(id));
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public List<WordingDto> listWordings() {
        return Json.map(templates.listWordings(), TemplateController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.WORDINGS_MODIFY)
    public WordingDto addWording(WordingInputDto input) {
        return toDto(templates.addWording(input.getText()));
    }

    @Override
    @PreAuthorize(Permissions.WORDINGS_MODIFY)
    public void deleteWording(Long id) {
        templates.deleteWording(id);
    }

    private static DocumentModelDetails toDetails(DocumentModelInputDto in) {
        return new DocumentModelDetails(in.getName(), Json.sameName(in.getType(), DocumentType.class), in.getTitle(),
                in.getContent(), Json.intOrZero(in.getAddressCount()),
                Json.map(in.getLayouts(), l -> new PageLayout(l.getLayout(), Json.isTrue(l.getSinglePage()))));
    }

    private static DocumentModelDto toDto(DocumentModel m) {
        DocumentModelDetails d = m.details();
        return new DocumentModelDto(m.id().value()).name(d.name()).type(Json.sameName(d.type(), DocumentTypeDto.class))
                .title(d.title()).content(d.content()).addressCount(d.addressCount())
                .layouts(Json.map(d.layouts(), l -> new PageLayoutDto().layout(l.layout()).singlePage(l.singlePage())));
    }

    private static WordingDto toDto(Wording w) {
        return new WordingDto().id(w.id()).text(w.text());
    }
}
