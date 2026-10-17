package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;

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

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, mode = EnumSource.Mode.EXCLUDE,
        names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED", "TRANSFERRED_TO_HIGH_COURT", "CLOSED"})
    void shouldShowTheTabForAnApprovedLiveWarrant(EnforcementStatus status) {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(warrant(status)));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowTheTabForAWarrantOfRestitution() {
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder()
            .status(AWAITING_SCHEDULING)
            .warrantOfRestitutionDetails(WarrantOfRestitutionEntity.builder().build())
            .build();
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(enforcement));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED",
        "CLOSED"})
    void shouldNotShowTheTabForAWarrantThatIsNotApprovedAndLive(EnforcementStatus status) {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(warrant(status)));
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_REVIEW", "TRANSFERRED_TO_HIGH_COURT"})
    void shouldNotShowTheTabForAWrit(EnforcementStatus status) {
        // A writ has no warrant record: its details live in the enforcement order JSON
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder()
            .status(status)
            .enforcementOrder(EnforcementOrder.builder().writDetails(WritDetails.builder().build()).build())
            .build();
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(enforcement));
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldOverwriteAStaleShowTabAnswerFromADraftApplication() {
        when(riskLevelService.findCurrentEnforcement(pcsCaseEntity))
            .thenReturn(Optional.of(warrant(AWAITING_REVIEW)));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .showRiskAssessmentTab(YesOrNo.YES)
                                  .warrantDetails(WarrantDetails.builder().build())
                                  .writDetails(WritDetails.builder().build())
                                  .build())
            .build();

        underTest.setRiskAssessmentFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status) {
        return EnforcementOrderEntity.builder()
            .status(status)
            .warrantDetails(WarrantEntity.builder().build())
            .build();
    }
}
