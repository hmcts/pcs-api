package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskLevelViewTest {

    @Mock
    private RiskLevelService riskLevelService;

    private RiskLevelView underTest;
    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @BeforeEach
    void setUp() {
        underTest = new RiskLevelView(riskLevelService);
    }

    @Test
    void shouldShowTheCurrentRiskLevelAndReason() {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(
            EnforcementOrderEntity.builder().riskLevel(RiskLevel.HIGH).riskLevelReason("Dog on site").build()));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        RiskLevelDetails details = pcsCase.getEnforcementOrder().getRiskLevelDetails();
        assertThat(details.getCurrentRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(details.getCurrentRiskLevelReason()).isEqualTo("Dog on site");
    }

    @Test
    void shouldClearAStaleRiskLevelWhenNoneIsSet() {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(
            new EnforcementOrderEntity()));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .riskLevelDetails(RiskLevelDetails.builder()
                                                        .currentRiskLevel(RiskLevel.MEDIUM)
                                                        .currentRiskLevelReason("old")
                                                        .build())
                                  .build())
            .build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getRiskLevelDetails().getCurrentRiskLevel()).isNull();
        assertThat(pcsCase.getEnforcementOrder().getRiskLevelDetails().getCurrentRiskLevelReason()).isNull();
    }

    @Test
    void shouldLeaveTheCaseAloneWhenTheClaimHasNoEnforcement() {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldDoNothingWithoutACase() {
        underTest.setRiskAssessmentFields(null, PCSCase.builder().build());
        underTest.setRiskAssessmentFields(pcsCaseEntity, null);

        verifyNoInteractions(riskLevelService);
    }
}
