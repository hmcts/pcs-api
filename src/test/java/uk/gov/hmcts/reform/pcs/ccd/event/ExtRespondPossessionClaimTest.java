package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

class ExtRespondPossessionClaimTest extends BaseEventTest {

    ExtRespondPossessionClaim extRespondPossessionClaim;

    @BeforeEach
    void setUp() {
        extRespondPossessionClaim = new ExtRespondPossessionClaim();
        setEventUnderTest(extRespondPossessionClaim);
    }

    @Test
    void shouldReturnDefaultSubmitResponse() {
        // given
        PCSCase pcsCase = PCSCase.builder().build();

        // when
        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        // then
        assertThat(submitResponse).isEqualTo(SubmitResponse.defaultResponse());
    }

    @Test
    void shouldBeConfiguredAsShowForFeatureFlagsAndLegalRepUpdatedDetails() {
        assertConfiguredShowConditions(ShowConditions.and(
            "legalRepUpdatedDetails=\"Yes\"",
            ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2)));
    }

    @Test
    void shouldBeConfiguredAsForCaseIssuedState() {
        assertConfiguredForStates(State.CASE_ISSUED);
    }

    @Test
    void shouldContainCorrectGrants() {
        assertGrants(UserRole.DEFENDANT_SOLICITOR, Permission.CRU);
    }

}
