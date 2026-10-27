package ch.conciliations.mabecoju.application.template;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Strings;
import ch.conciliations.mabecoju.domain.template.DocumentModel;
import ch.conciliations.mabecoju.domain.template.DocumentModelDetails;
import ch.conciliations.mabecoju.domain.template.DocumentModelId;
import ch.conciliations.mabecoju.domain.template.DocumentModelRepository;
import ch.conciliations.mabecoju.domain.template.DocumentType;
import ch.conciliations.mabecoju.domain.template.Wording;
import ch.conciliations.mabecoju.domain.template.WordingRepository;
import jakarta.transaction.Transactional;

import java.util.List;

/** Maintaining document models and predefined honorary wordings. */
@Transactional
public class TemplateUseCases {

    private final DocumentModelRepository models;
    private final WordingRepository wordings;

    public TemplateUseCases(DocumentModelRepository models, WordingRepository wordings) {
        this.models = models;
        this.wordings = wordings;
    }

    public DocumentModel createModel(DocumentModelDetails details) {
        requireUniqueName(details.name(), null);
        return models.save(DocumentModel.create(details));
    }

    public DocumentModel updateModel(DocumentModelId id, DocumentModelDetails details) {
        DocumentModel model = getModel(id);
        requireUniqueName(details.name(), id);
        model.update(details);
        return models.save(model);
    }

    public void deleteModel(DocumentModelId id) {
        getModel(id);
        models.delete(id);
    }

    public DocumentModel getModel(DocumentModelId id) {
        return Ids.require(models.findById(id), "Document model", id.value());
    }

    /** @param type null for every type */
    public List<DocumentModel> listModels(DocumentType type) {
        return type == null ? models.findAll() : models.findByType(type);
    }

    public List<Wording> listWordings() {
        return wordings.findAll();
    }

    public Wording addWording(String text) {
        String trimmed = Strings.requireNotBlank(text, "Wording");
        Strings.requireMaxLength(trimmed, 80, "Wording");
        return wordings.add(trimmed);
    }

    public void deleteWording(long id) {
        wordings.delete(id);
    }

    private void requireUniqueName(String name, DocumentModelId self) {
        models.findByName(name)
                .filter(existing -> !existing.id().equals(self))
                .ifPresent(existing -> {
                    throw new DomainException(ErrorCode.MODEL_NAME_EXISTS, "A model named '" + name + "' already exists")
                            .with("name", name);
                });
    }
}
