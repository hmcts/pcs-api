package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCategoryUpdateServiceTest {

    private static final long CASE_REFERENCE = 12345L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentRepository documentRepository;

    private DocumentCategoryUpdateService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentCategoryUpdateService(pcsCaseService, documentRepository);
    }

    @Test
    void shouldPersistSuppliedCategoriesAndPreserveDocumentDetails() {
        DocumentEntity first = document("propertyDocuments");
        DocumentEntity second = document("evidence");
        existingCase(first, second);

        underTest.updateCategories(
            CASE_REFERENCE, List.of(submitted(first, "correspondence"), submitted(second, null)));

        assertThat(first.getCategoryId()).isEqualTo("correspondence");
        assertThat(second.getCategoryId()).isNull();
        assertThat(first.getFileName()).isEqualTo("original.pdf");
        assertThat(first.getUrl()).isEqualTo("http://documents/original");
        verify(documentRepository).save(first);
        verify(documentRepository).save(second);
        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verifyNoMoreInteractions(pcsCaseService, documentRepository);
    }

    @Test
    void shouldLeaveUnchangedAndOmittedDocuments() {
        DocumentEntity unchanged = document("evidence");
        DocumentEntity omitted = document("propertyDocuments");
        existingCase(unchanged, omitted);

        underTest.updateCategories(CASE_REFERENCE, List.of(submitted(unchanged, "evidence")));

        assertThat(omitted.getCategoryId()).isEqualTo("propertyDocuments");
        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldOnlyUpdateDocumentsOnCase() {
        DocumentEntity document = document("evidence");
        existingCase(document);

        underTest.updateCategories(CASE_REFERENCE, List.of(submitted(document("evidence"), "correspondence")));

        assertThat(document.getCategoryId()).isEqualTo("evidence");
        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldAcceptAnEmptyDocumentList() {
        existingCase(document("evidence"));

        underTest.updateCategories(CASE_REFERENCE, List.of());

        verifyNoInteractions(documentRepository);
    }

    private void existingCase(DocumentEntity... documents) {
        PcsCaseEntity entity = new PcsCaseEntity();
        entity.getDocuments().addAll(List.of(documents));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(entity);
    }

    private DocumentEntity document(String category) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setCategoryId(category);
        entity.setFileName("original.pdf");
        entity.setUrl("http://documents/original");
        return entity;
    }

    private ListValue<Document> submitted(DocumentEntity entity, String category) {
        return ListValue.<Document>builder().id(entity.getId().toString())
            .value(Document.builder().categoryId(category).filename("ignored.pdf").build()).build();
    }
}
