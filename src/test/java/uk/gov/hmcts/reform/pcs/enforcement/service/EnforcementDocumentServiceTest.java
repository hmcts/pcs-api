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
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private PcsCaseEntity pcsCaseEntity;
    private EnforcementOrderEntity enforcementOrderEntity;

    private EnforcementDocumentService underTest;

    @BeforeEach
    void setUp() {
        DocumentService documentService = new DocumentService(documentRepository, documentIdExtractor,
                                                              documentNameService, documentTypeMapper,
                                                              documentCategoryMapper, taskDescriptionService,
                                                              camundaService);
        underTest = new EnforcementDocumentService(documentService);

        pcsCaseEntity = PcsCaseEntity.builder().build();
        enforcementOrderEntity = EnforcementOrderEntity.builder()
            .claim(ClaimEntity.builder().pcsCase(pcsCaseEntity).build())
            .build();
    }

    @ParameterizedTest
    @EnumSource(EvidenceDocumentType.class)
    void shouldMapAllEvidenceDocumentTypes(EvidenceDocumentType evidenceDocumentType) {
        // Given
        when(documentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        EnforcementOrder enforcementOrder = enforcementOrderWithEvidence(evidenceDocumentType);

        // When
        underTest.createAllDocuments(enforcementOrderEntity, enforcementOrder);

        // Then
        DocumentType expectedDocumentType = DocumentType.valueOf(evidenceDocumentType.name());

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        assertThat(documentEntityListCaptor.getValue())
                .extracting(DocumentEntity::getType)
                .containsExactly(expectedDocumentType);
    }

    @ParameterizedTest
    @EnumSource(EvidenceDocumentType.class)
    void shouldFileEveryEvidenceDocumentInTheEnforcementFolder(EvidenceDocumentType evidenceDocumentType) {
        // Given
        when(documentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        EnforcementOrder enforcementOrder = enforcementOrderWithEvidence(evidenceDocumentType);

        // When
        List<DocumentEntity> entities = underTest.createAllDocuments(enforcementOrderEntity, enforcementOrder);

        // Then
        assertThat(entities)
            .extracting(DocumentEntity::getCategoryId)
            .containsExactly(FeatureCaseFileCategory.ENFORCEMENT.getId());
    }

    @Test
    void shouldAttachTheDocumentsToTheCaseAndTheEnforcementOrder() {
        // Given
        when(documentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        EnforcementOrder enforcementOrder = enforcementOrderWithEvidence(EvidenceDocumentType.POLICE_REPORT);

        // When
        List<DocumentEntity> entities = underTest.createAllDocuments(enforcementOrderEntity, enforcementOrder);

        // Then
        assertThat(entities).hasSize(1);
        DocumentEntity document = entities.getFirst();
        assertThat(document.getPcsCase()).isSameAs(pcsCaseEntity);
        assertThat(pcsCaseEntity.getDocuments()).containsExactly(document);
        assertThat(enforcementOrderEntity.getDocuments()).containsExactly(document);
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
        List<DocumentEntity> entities = underTest.createAllDocuments(enforcementOrderEntity, enforcementOrder);

        // Then
        assertThat(entities).isEmpty();
        verify(documentRepository).saveAll(List.of());
        assertThat(pcsCaseEntity.getDocuments()).isEmpty();
        assertThat(enforcementOrderEntity.getDocuments()).isEmpty();
    }

    private static EnforcementOrder enforcementOrderWithEvidence(EvidenceDocumentType evidenceDocumentType) {
        EvidenceOfDefendants evidenceDocument = EvidenceOfDefendants.builder()
                .document(Document.builder()
                        .url("url-" + evidenceDocumentType + "/bf112cdf-76d7-4d15-bb92-cd7c3483a7ef")
                        .filename("file-" + evidenceDocumentType)
                        .binaryUrl("bin-" + evidenceDocumentType)
                        .build())
                .documentType(evidenceDocumentType)
                .build();

        return EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(List.of(ListValue.<EvidenceOfDefendants>builder()
                                .id("1")
                                .value(evidenceDocument)
                                .build()))
                        .build())
                .build();
    }
}
