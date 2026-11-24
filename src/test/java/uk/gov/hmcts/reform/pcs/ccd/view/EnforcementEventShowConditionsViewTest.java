package uk.gov.hmcts.reform.pcs.ccd.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ASSESS_RISK_LEVEL;

@ExtendWith(MockitoExtension.class)
class EnforcementEventShowConditionsViewTest {

    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private EnforcementEventShowConditionsView enforcementEventShowConditionsView;

    @BeforeEach
    void enableRiskAssessment() {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSerializeNestedCamelCaseConditionsForBothFlagStates(boolean enabled) {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(enabled);
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity entity = PcsCaseEntity.builder().build();
        EnforcementOrderEntity application = EnforcementOrderEntity.builder()
            .state(EnforcementState.AWAITING_BAILIFF_ALLOCATION).build();
        when(enforcementOrderMediator.getEnforcementOrder(entity)).thenReturn(Optional.of(application));
        if (enabled) {
            when(enforcementOrderMediator.getLatestSubmittedWarrantApplication(entity))
                .thenReturn(Optional.of(application));
        }

        enforcementEventShowConditionsView.setCaseFields(entity, pcsCase);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.valueToTree(pcsCase);
        JsonNode conditions = data.path("enfShowConditions");
        assertThat(conditions.path("recordWarrantReturn")).isEqualTo(mapper.valueToTree(YesOrNo.YES));
        assertThat(conditions.path("canViewRiskAssessment"))
            .isEqualTo(mapper.valueToTree(YesOrNo.from(enabled)));
        assertThat(conditions.has("RecordWarrantReturn")).isFalse();
        assertThat(conditions.has("CanViewRiskAssessment")).isFalse();
        assertThat(data.has("enforcementEventShowConditions")).isFalse();
        assertThat(data.has("enfShowConditions_RecordWarrantReturn")).isFalse();
        assertThat(data.has("enfShowConditions_CanViewRiskAssessment")).isFalse();
    }

    @ParameterizedTest
    @MethodSource("recordWarrantReturnScenarios")
    void shouldPreserveWarrantReturnEligibilityAndSkipRiskAssessmentWhenFlagIsOff(EnforcementState state,
                                                                               YesOrNo expected) {
        when(featureToggleService.isEnabled(ASSESS_RISK_LEVEL)).thenReturn(false);
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity entity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(entity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().state(state).build()));

        enforcementEventShowConditionsView.setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnfShowConditions().getRecordWarrantReturn()).isEqualTo(expected);
        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
        verify(enforcementOrderMediator, never()).getLatestSubmittedWarrantApplication(entity);
    }

    @ParameterizedTest
    @MethodSource("recordWarrantReturnScenarios")
    void shouldSetRecordWarrantReturnFromActiveEnforcementState(EnforcementState enforcementState,
                                                                   YesOrNo expected) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().state(enforcementState).build()));

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnfShowConditions().getRecordWarrantReturn()).isEqualTo(expected);
    }

    @Test
    void shouldNotAllowRecordWarrantReturnWhenNoEnforcementState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity)).thenReturn(Optional.empty());

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnfShowConditions().getRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldNotAllowRecordWarrantReturnWhenEnforcementOrderHasNoState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().build()));

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnfShowConditions().getRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
    }

    @ParameterizedTest
    @MethodSource("recordWarrantReturnScenarios")
    void shouldSetRiskAssessmentVisibilityFromTheSubmittedWarrantState(EnforcementState state, YesOrNo expected) {
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity entity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getLatestSubmittedWarrantApplication(entity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().state(state).build()));

        enforcementEventShowConditionsView.setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(expected);
    }

    @Test
    void shouldHideRiskAssessmentWithoutASubmittedWarrant() {
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity entity = PcsCaseEntity.builder().build();

        enforcementEventShowConditionsView.setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldHideRiskAssessmentWhenTheSubmittedWarrantHasNoState() {
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity entity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getLatestSubmittedWarrantApplication(entity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().build()));

        enforcementEventShowConditionsView.setCaseFields(entity, pcsCase);

        assertThat(pcsCase.getEnfShowConditions().getCanViewRiskAssessment()).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> recordWarrantReturnScenarios() {
        return Stream.of(
            Arguments.of(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS, YesOrNo.NO),
            Arguments.of(EnforcementState.PENDING_CASE_ISSUED, YesOrNo.NO),
            Arguments.of(EnforcementState.CASE_PROGRESSION, YesOrNo.YES),
            Arguments.of(EnforcementState.BREATHING_SPACE, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_ENFORCEMENT_OUTCOME, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_PROPERTY_VISIT, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_SCHEDULING, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_BAILIFF_ALLOCATION, YesOrNo.YES),
            Arguments.of(EnforcementState.JUDICIAL_REFERRAL, YesOrNo.YES),
            Arguments.of(EnforcementState.HEARING_READINESS, YesOrNo.YES),
            Arguments.of(EnforcementState.PREPARE_FOR_HEARING_CONDUCT_HEARING, YesOrNo.YES),
            Arguments.of(EnforcementState.CLOSED, YesOrNo.YES),
            Arguments.of(EnforcementState.CASE_STAYED, YesOrNo.YES),
            Arguments.of(EnforcementState.REQUESTED_FOR_DELETION, YesOrNo.NO),
            Arguments.of(EnforcementState.DRAFT_DISCARDED, YesOrNo.NO)
        );
    }
}
