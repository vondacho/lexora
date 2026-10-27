package io.lexora.infrastructure.persistence.template;

import io.lexora.domain.template.DocumentModel;
import io.lexora.domain.template.DocumentModelDetails;
import io.lexora.domain.template.DocumentModelId;
import io.lexora.domain.template.DocumentModelRepository;
import io.lexora.domain.template.DocumentType;
import io.lexora.domain.template.PageLayout;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.isTrue;
import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class DocumentModelRepositoryAdapter implements DocumentModelRepository {

    private final DocumentModelJpaRepository jpa;

    DocumentModelRepositoryAdapter(DocumentModelJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<DocumentModel> findById(DocumentModelId id) {
        return jpa.findById(toInt(id.value())).map(DocumentModelRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<DocumentModel> findByName(String name) {
        return jpa.findFirstByName(name).map(DocumentModelRepositoryAdapter::toDomain);
    }

    @Override
    public List<DocumentModel> findAll() {
        return jpa.findAllByOrderByNameAscTypeAsc().stream().map(DocumentModelRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<DocumentModel> findByType(DocumentType type) {
        return jpa.findByTypeOrderByNameAsc(type.code()).stream().map(DocumentModelRepositoryAdapter::toDomain).toList();
    }

    @Override
    public DocumentModel save(DocumentModel model) {
        DocumentModelEntity e = model.id() == null
                ? new DocumentModelEntity()
                : jpa.findById(toInt(model.id().value())).orElseThrow();
        DocumentModelDetails d = model.details();
        e.name = d.name();
        e.type = d.type().code();
        e.title = d.title();
        e.content = d.content();
        e.addressCount = d.addressCount();
        List<PageLayout> layouts = d.layouts();
        e.layout1 = layouts.get(0).layout();
        e.layout2 = layouts.get(1).layout();
        e.layout3 = layouts.get(2).layout();
        e.layout4 = layouts.get(3).layout();
        e.singlePage1 = layouts.get(0).singlePage();
        e.singlePage2 = layouts.get(1).singlePage();
        e.singlePage3 = layouts.get(2).singlePage();
        e.singlePage4 = layouts.get(3).singlePage();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(DocumentModelId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static DocumentModel toDomain(DocumentModelEntity e) {
        return DocumentModel.restore(DocumentModelId.of(e.id), new DocumentModelDetails(e.name,
                DocumentType.fromCode(e.type), e.title, e.content, e.addressCount == null ? 0 : e.addressCount,
                List.of(new PageLayout(e.layout1, isTrue(e.singlePage1)),
                        new PageLayout(e.layout2, isTrue(e.singlePage2)),
                        new PageLayout(e.layout3, isTrue(e.singlePage3)),
                        new PageLayout(e.layout4, isTrue(e.singlePage4)))));
    }
}
