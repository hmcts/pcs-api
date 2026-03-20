package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.RiskProfileService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.StatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WarrantOfRestitutionMapper;

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
    private StatementOfTruthMapper statementOfTruthMapper;

    @Test
    void shouldProcessWarrantOfRestitutionDetailsAndSaveToRepository() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity;
        EnforcementOrder enforcementOrder;
        WarrantOfRestitutionDetails warrantOfRestitutionDetails;
        enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        warrantOfRestitutionDetails = WarrantOfRestitutionDetails.builder().anyRiskToBailiff(YesNoNotSure.YES).build();

        VulnerableAdultsChildren vulnerableAdultsChildren = VulnerableAdultsChildren.builder()
                .vulnerableCategory(VULNERABLE_CHILDREN).vulnerableReasonText("Young children present").build();

        RawWarrantRestDetails rawWarrantRestDetails = RawWarrantRestDetails.builder()
                .vulnerablePeoplePresentWarrantRest(YesNoNotSure.YES)
                .vulnerableAdultsChildrenWarrantRest(vulnerableAdultsChildren).build();

        enforcementOrder = EnforcementOrder.builder().warrantOfRestitutionDetails(warrantOfRestitutionDetails)
                .rawWarrantRestDetails(rawWarrantRestDetails).build();
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = WarrantOfRestitutionEntity.builder().build();
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
        verify(statementOfTruthMapper).mapStatementOfTruthForWarrantRest(enforcementOrder, enforcementOrderEntity);
        WarrantOfRestitutionEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcementOrderEntity);
    }
}
