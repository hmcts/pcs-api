package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.risklevel.SetRiskLevelConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@ExtendWith(MockitoExtension.class)
class SetRiskLevelTest extends BaseEventTest {

    private static final String WARRANT_NUMBER = "WP-00000001";

    @Mock
    private SetRiskLevelConfigurer setRiskLevelConfigurer;
    @Mock
    private RiskLevelService riskLevelService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new SetRiskLevel(setRiskLevelConfigurer, riskLevelService, new CaseReferenceFormatter()));
    }

    @Test
    void shouldConfigureThePages() {
        verify(setRiskLevelConfigurer).configurePages(any());
    }

    @Test
    void shouldOnlyBeOfferedForAnApprovedLiveWarrant() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\" AND canSetRiskLevel=\"Yes\"");
    }

    @Test
    void shouldBeAvailableInEveryStateOnceTheCaseIsIssued() {
        assertConfiguredForStates(AddBailiffNote.STATES);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER", "HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER"})
    void shouldGrantCrudToBailiffStaff(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN"})
    void shouldGiveThePartiesNoAccess(UserRole userRole) {
        assertThat(configuredEvent.getGrants().get(userRole)).isEmpty();
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role)).as("Grants for role %s", role)
                .containsExactly(Permission.R);
        }
    }

    @Test
    void shouldSubmitStraightFromTheRiskLevelPage() {
        assertThat(configuredEvent.getEndButtonLabel()).isEqualTo("Continue");
        assertThat(configuredEvent.isShowSummary()).isFalse();
    }

    @Test
    void shouldRefuseToStartWhenTheRiskLevelCannotBeSet() {
        when(riskLevelService.getEnforcementForRiskLevel(TEST_CASE_REFERENCE))
            .thenThrow(new EnforcementEventNotAvailableException("not yet"));

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
    }

    @Test
    void shouldPreSelectTheCurrentRiskLevelAndReason() {
        when(riskLevelService.getEnforcementForRiskLevel(TEST_CASE_REFERENCE)).thenReturn(
            EnforcementOrderEntity.builder().riskLevel(RiskLevel.HIGH).riskLevelReason("Dog on site").build());

        PCSCase started = callStartHandler(PCSCase.builder().build());

        RiskLevelDetails details = started.getEnforcementOrder().getRiskLevelDetails();
        assertThat(details.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(details.getRiskLevelReason()).isEqualTo("Dog on site");
    }

    @Test
    void shouldStartWithNothingSelectedWhenNoLevelIsSet() {
        when(riskLevelService.getEnforcementForRiskLevel(TEST_CASE_REFERENCE))
            .thenReturn(new EnforcementOrderEntity());
        PCSCase caseData = caseWith(RiskLevel.MEDIUM, "stale");

        PCSCase started = callStartHandler(caseData);

        RiskLevelDetails details = started.getEnforcementOrder().getRiskLevelDetails();
        assertThat(details.getRiskLevel()).isNull();
        assertThat(details.getRiskLevelReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(RiskLevel.class)
    void shouldSaveTheRiskLevelAndConfirmIt(RiskLevel level) {
        when(riskLevelService.setRiskLevel(anyLong(), any(), anyString()))
            .thenReturn(EnforcementChange.made(WARRANT_NUMBER));

        SubmitResponse<State> response = callSubmitHandler(caseWith(level, "Known history of violence"));

        verify(riskLevelService).setRiskLevel(TEST_CASE_REFERENCE, level, "Known history of violence");
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# Risk level set to " + level.getLabel());
        assertThat(response.getConfirmationBody())
            .isEqualTo("Case number: 0000-0000-0000-1234\n\nWarrant number: " + WARRANT_NUMBER);
    }

    @Test
    void shouldAskForARiskLevel() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly("Choose a risk level");
        verify(riskLevelService, never()).setRiskLevel(anyLong(), any(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldAskWhyTheLevelIsHigh(String reason) {
        SubmitResponse<State> response = callSubmitHandler(caseWith(RiskLevel.HIGH, reason));

        assertThat(response.getErrors()).containsExactly("Enter why you set the risk level to high");
    }

    @Test
    void shouldAcceptAReasonOf6800CharactersAndRefuseALongerOne() {
        when(riskLevelService.setRiskLevel(anyLong(), any(), anyString()))
            .thenReturn(EnforcementChange.made(WARRANT_NUMBER));

        assertThat(callSubmitHandler(caseWith(RiskLevel.HIGH, "a".repeat(6800))).getErrors()).isNullOrEmpty();
        assertThat(callSubmitHandler(caseWith(RiskLevel.HIGH, "a".repeat(6801))).getErrors())
            .containsExactly("The reason must be 6,800 characters or fewer");
    }

    @Test
    void shouldNotNeedAReasonForMedium() {
        when(riskLevelService.setRiskLevel(TEST_CASE_REFERENCE, RiskLevel.MEDIUM, null))
            .thenReturn(EnforcementChange.made(WARRANT_NUMBER));

        SubmitResponse<State> response = callSubmitHandler(caseWith(RiskLevel.MEDIUM, null));

        assertThat(response.getErrors()).isNullOrEmpty();
        verify(riskLevelService).setRiskLevel(TEST_CASE_REFERENCE, RiskLevel.MEDIUM, null);
    }

    @Test
    void shouldShowTheErrorWhenTheRiskLevelCanNoLongerBeSet() {
        when(riskLevelService.setRiskLevel(TEST_CASE_REFERENCE, RiskLevel.MEDIUM, null))
            .thenReturn(EnforcementChange.refused("closed"));

        SubmitResponse<State> response = callSubmitHandler(caseWith(RiskLevel.MEDIUM, null));

        assertThat(response.getErrors()).containsExactly("closed");
        assertThat(response.getConfirmationHeader()).isNull();
    }

    private static PCSCase caseWith(RiskLevel level, String reason) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .riskLevelDetails(RiskLevelDetails.builder()
                                                        .riskLevel(level)
                                                        .riskLevelReason(reason)
                                                        .build())
                                  .build())
            .build();
    }
}
