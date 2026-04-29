package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory.VULNERABLE_CHILDREN;

@ExtendWith(MockitoExtension.class)
class WarrantOfRestitutionStrategyTest {

    @InjectMocks
    private WarrantOfRestitutionStrategy underTest;
    @Mock
    private RiskProfileService riskProfileService;
    @Mock
    private WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    @Mock
    private WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private DocumentService documentService;

    @Test
    void shouldProcessWarrantOfRestitutionDetailsAndSaveToRepository() {
        // Given
        WarrantOfRestitutionDetails warrantOfRestitutionDetails;
        EnforcementOrderEntity enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        RawWarrantRestDetails rawWarrantRestDetails = RawWarrantRestDetails.builder()
                .vulnerablePeoplePresentWarrantRest(YesNoNotSure.YES)
                .vulnerableAdultsChildrenWarrantRest(vulnerableAdultsChildren).build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(warrantOfRestitutionDetails)
                .rawWarrantRestDetails(rawWarrantRestDetails).build();
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = WarrantOfRestitutionEntity.builder()
                .enforcementOrder(enforcementOrderEntity)
                .build();
        when(warrantOfRestitutionMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(warrantOfRestitutionEntity);
        when(warrantOfRestitutionRepository.save(warrantOfRestitutionEntity)).thenReturn(warrantOfRestitutionEntity);
        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);
        ArgumentCaptor<WarrantOfRestitutionEntity> captor =
            ArgumentCaptor.forClass(WarrantOfRestitutionEntity.class);

        // Then
        verify(riskProfileService).processRisk(enforcementOrder, enforcementOrderEntity);
        verify(warrantOfRestitutionRepository).save(captor.capture());
        WarrantOfRestitutionEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcementOrderEntity);
    }

    @Test
    void shouldProcessDocumentsAndSaveToRepository() {
        // Given
        EvidenceOfDefendants evidenceDocument = EvidenceOfDefendants.builder()
                .document(Document.builder()
                        .url("url-WITNESS_STATEMENT")
                        .filename("file-WITNESS_STATEMENT")
                        .binaryUrl("bin-WITNESS_STATEMENT")
                        .categoryId("cat-WITNESS_STATEMENT")
                        .build())
                .documentType(EvidenceDocumentType.WITNESS_STATEMENT)
                .build();

        List<ListValue<EvidenceOfDefendants>> evidenceDocuments =
                List.of(ListValue.<EvidenceOfDefendants>builder()
                .id("1").value(evidenceDocument).build());

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(evidenceDocuments)
                        .build())
                .build();
        EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder().build();

        WarrantOfRestitutionEntity warrantOfRestitutionEntity = WarrantOfRestitutionEntity.builder().build();
        List<DocumentEntity> documentEntities = List.of(DocumentEntity.builder().build());
        enforcementOrderEntity.addDocuments(documentEntities);

        when(warrantOfRestitutionMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(warrantOfRestitutionEntity);
        when(warrantOfRestitutionRepository.save(warrantOfRestitutionEntity)).thenReturn(warrantOfRestitutionEntity);
        when(documentService.createAllDocuments(enforcementOrder)).thenReturn(documentEntities);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntity);
    }
}
