package uk.gov.hmcts.reform.pcs.ccd.event.changecasestate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseStateOption;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class ChangeCaseStateTest extends BaseEventTest {

    private static final String FORMATTED_ADDRESS = "1 Test Street, London, SW1A 1AA";

    @Mock
    private AddressFormatter addressFormatter;

    @Mock
    private CamundaService camundaService;

    @InjectMocks
    private ChangeCaseState changeCaseState;

    @BeforeEach
    void setUp() {
        setEventUnderTest(changeCaseState);
    }

    @Test
    void shouldConfigureCaseworkerEventAccessAndStates() {
        assertThat(configuredEvent.getPreState()).containsExactlyInAnyOrder(
            State.CASE_ISSUED,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.CASE_PROGRESSION,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CASE_STAYED,
            State.BREATHING_SPACE
        );
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_TEAM_LEADER))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U, Permission.D);
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_ADMIN))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U, Permission.D);
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_TEAM_LEADER)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CIRCUIT_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.FEE_PAID_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.LEADERSHIP_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_TEAM_LEADER)).containsExactly(Permission.R);
    }

    @ParameterizedTest
    @EnumSource(CaseStateOption.class)
    void shouldTransitionToSelectedTargetState(CaseStateOption targetStateOption) {
        stubFormattedAddress();
        PCSCase pcsCase = PCSCase.builder()
            .targetState(targetStateOption)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        State targetState = targetStateOption.toState();
        assertThat(response.getState()).isEqualTo(targetState);

        if (targetState == State.CASE_STAYED) {
            verify(camundaService).cancelTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        } else {
            verify(camundaService, never()).cancelTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        }
    }

    @Test
    void shouldIncludeCaseReferenceInConfirmationBody() {
        stubFormattedAddress();
        PCSCase pcsCase = PCSCase.builder()
            .targetState(CaseStateOption.JUDICIAL_REFERRAL)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getConfirmationBody())
            .contains(String.valueOf(TEST_CASE_REFERENCE));
    }

    @Test
    void shouldIncludeFormattedAddressInConfirmationBody() {
        stubFormattedAddress();
        PCSCase pcsCase = PCSCase.builder()
            .targetState(CaseStateOption.HEARING_READINESS)
            .build();

        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        assertThat(response.getConfirmationBody()).contains(FORMATTED_ADDRESS);
    }

    private void stubFormattedAddress() {
        when(addressFormatter.formatShortAddress(isNull(), eq(COMMA_DELIMITER)))
            .thenReturn(FORMATTED_ADDRESS);
    }
}
