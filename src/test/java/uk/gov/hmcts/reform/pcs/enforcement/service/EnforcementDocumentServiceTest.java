package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentTypeMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private DocumentTypeMapper documentTypeMapper;
    @Mock
    private DocumentCategoryMapper documentCategoryMapper;
    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private CamundaService camundaService;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentEntityListCaptor;

    private EnforcementDocumentService underTest;

    @BeforeEach
    void setUp() {
        DocumentService documentService = new DocumentService(documentRepository, documentIdExtractor,
                                                              documentNameService, documentTypeMapper,
                                                              documentCategoryMapper, taskDescriptionService,
                                                              camundaService);
        underTest = new EnforcementDocumentService(documentService);
    }

    @ParameterizedTest
    @EnumSource(EvidenceDocumentType.class)
    void shouldMapAllEvidenceDocumentTypes(EvidenceDocumentType evidenceDocumentType) {
        // Given
        EvidenceOfDefendants evidenceDocument = EvidenceOfDefendants.builder()
                .document(Document.builder()
                        .url("url-WITNESS_STATEMENT/bf112cdf-76d7-4d15-bb92-cd7c3483a7ef")
                        .filename("file-WITNESS_STATEMENT")
                        .binaryUrl("bin-WITNESS_STATEMENT")
                        .categoryId("cat-WITNESS_STATEMENT")
                        .build())
                .documentType(evidenceDocumentType)
                .build();

        List<ListValue<EvidenceOfDefendants>> evidenceDocuments =
                List.of(ListValue.<EvidenceOfDefendants>builder()
                        .id("1").value(evidenceDocument).build());

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(evidenceDocuments)
                        .build())
                .build();

        // When
        underTest.createAllDocuments(enforcementOrder);

        // Then
        DocumentType expectedDocumentType = DocumentType.valueOf(evidenceDocumentType.name());

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities)
                .extracting(DocumentEntity::getType)
                .containsExactly(expectedDocumentType);
    }

    @Test
    void shouldPersistNullCategoryForPoliceReportEvidenceDocuments() {
        EvidenceOfDefendants evidenceDocument = EvidenceOfDefendants.builder()
            .document(Document.builder()
                .url("url-POLICE_REPORT")
                .filename("file-POLICE_REPORT")
                .binaryUrl("bin-POLICE_REPORT")
                .build())
            .documentType(EvidenceDocumentType.POLICE_REPORT)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                .additionalDocuments(List.of(ListValue.<EvidenceOfDefendants>builder()
                    .id("1")
                    .value(evidenceDocument)
                    .build()))
                .build())
            .build();

        underTest.createAllDocuments(enforcementOrder);

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities).hasSize(1);
        assertThat(capturedEntities.getFirst().getCategoryId())
            .isEqualTo(CaseFileCategory.UNCATEGORISED_DOCUMENTS.getId());
    }

    @Test
    void shouldHandleEmptyEvidenceDocument() {
        List<ListValue<EvidenceOfDefendants>> evidenceDocuments =
                new ArrayList<>();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(evidenceDocuments)
                        .build())
                .build();
        when(documentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<DocumentEntity> entities = underTest.createAllDocuments(enforcementOrder);

        // Then
        assertThat(entities).isEmpty();
        verify(documentRepository).saveAll(List.of());
    }
}
