package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;

@ExtendWith(MockitoExtension.class)
class DecideEnforcementTest extends BaseEventTest {

    @Mock
    private DecideEnforcementStartHandler startHandler;
    @Mock
    private DecideEnforcementSubmitHandler submitHandler;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new DecideEnforcement(startHandler, submitHandler));
    }

    @Test
    void shouldOnlyBeOfferedWhileAnApplicationIsAwaitingReview() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\" AND canDecideEnforcement=\"Yes\"");
    }

    @Test
    void shouldBeAvailableInTheStatesAnEnforcementApplicationLeavesTheCaseIn() {
        assertConfiguredForStates(State.CASE_ISSUED, State.PENDING_ENFORCEMENT_ISSUED);
    }

    @Test
    void shouldBeGrantedToHearingCentreCaseworkers() {
        Set<Permission> createReadUpdate = Set.of(C, R, U);
        assertGrants(UserRole.HEARING_CENTRE_ADMIN, createReadUpdate);
        assertGrants(UserRole.HEARING_CENTRE_TEAM_LEADER, createReadUpdate);
    }

    @Test
    void shouldNotBeGrantedToTheClaimantSide() {
        assertThat(configuredEventGrants()).doesNotContainKeys(
            UserRole.PCS_SOLICITOR, UserRole.GA_CLAIMANT_SOLICITOR, UserRole.CLAIMANT);
    }

    @Test
    void shouldShowTheHeaderAndTheDecisionFields() {
        assertThat(getEventFieldIds()).containsExactly(
            "formattedPropertyAddress",
            "formattedCaseName",
            "decideEnforcement-caseDetails",
            "enforcementDecision",
            "enforcementRejectionReason");
    }

    @Test
    void shouldUseTheStartHandler() {
        PCSCase caseData = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();
        when(startHandler.start(any())).thenReturn(caseData);

        assertThat(callStartHandler(caseData)).isSameAs(caseData);
    }

    @Test
    void shouldUseTheSubmitHandler() {
        PCSCase caseData = PCSCase.builder().build();
        SubmitResponse<State> response = SubmitResponse.defaultResponse();
        when(submitHandler.submit(any())).thenReturn(response);

        assertThat(callSubmitHandler(caseData)).isSameAs(response);
        verify(submitHandler).submit(new EventPayload<>(TEST_CASE_REFERENCE, caseData, null));
    }

    private java.util.Map<UserRole, ?> configuredEventGrants() {
        return configuredEvent.getGrants().asMap();
    }
}
