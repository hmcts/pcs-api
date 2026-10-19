package uk.gov.hmcts.reform.pcs.enforcement.service.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDocumentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatementOfTruthService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskProfileService;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.WarrantOfRestitutionMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory.VULNERABLE_CHILDREN;

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
    private EnforcementDocumentService enforcementDocumentService;
    @Mock
    private EnforcementStatementOfTruthService enforcementStatementOfTruthService;

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

        when(warrantOfRestitutionMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(warrantOfRestitutionEntity);
        when(warrantOfRestitutionRepository.save(warrantOfRestitutionEntity)).thenReturn(warrantOfRestitutionEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementDocumentService).createAllDocuments(enforcementOrderEntity, enforcementOrder);
        verify(enforcementOrderRepository).save(enforcementOrderEntity);
    }

    /**
     * The journey signs the warrant journey's statement of truth page, so the answers arrive on the warrant
     * details of a warrant of restitution application (HDPI-7555).
     */
    @Test
    void shouldStoreTheStatementOfTruthSignedOnTheWarrantPage() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantDetails(WarrantDetails.builder().statementOfTruth(statementOfTruth).build())
                .build();
        EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder().build();
        when(warrantOfRestitutionMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(WarrantOfRestitutionEntity.builder().build());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementStatementOfTruthService).processStatementOfTruth(statementOfTruth, enforcementOrderEntity);
    }

    @Test
    void shouldStoreNoStatementOfTruthWhenTheApplicationNeverReachedThePage() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder().build();
        when(warrantOfRestitutionMapper.toEntity(enforcementOrder, enforcementOrderEntity))
                .thenReturn(WarrantOfRestitutionEntity.builder().build());

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(enforcementStatementOfTruthService).processStatementOfTruth(null, enforcementOrderEntity);
    }
}
