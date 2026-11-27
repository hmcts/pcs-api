package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;

// Welsh fee confirmation proof of concept: not for merge.
class WelshPocFeeConfirmationTest extends BaseEventTest {

    @BeforeEach
    void setUp() {
        setEventUnderTest(new WelshPocFeeConfirmation());
    }

    @Test
    void shouldSetTheFeeThePlaceholdersPointAtOnStart() {
        // When
        PCSCase caseData = callStartHandler(PCSCase.builder().build());

        // Then
        assertThat(caseData.getWelshPocFeeAmount()).isEqualTo("40400");
    }

    @Test
    void shouldReturnTheTemplateAsTheHeaderAndTheFilledInSentenceAsTheBody() {
        // When
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        // Then
        assertThat(response.getConfirmationHeader())
            .isEqualTo("TEST- Pay ${welshPocFeeAmount} fee for case ${[CASE_REFERENCE]}");
        assertThat(response.getConfirmationBody()).isEqualTo("TEST- Pay £404.00 fee for case 1234");
        assertThat(response.getState()).isNull();
    }

    @Test
    void shouldPutTheFeeAndTheTemplateLabelOnThePageForClaimantUsersInEveryState() {
        assertThat(getEventFieldIds()).containsExactly("welshPocFeeAmount", "welshPocFeeLabel");
        // forAllStates() leaves the pre-states empty, which the definition writes as "*".
        assertThat(configuredEvent.getPreState()).isEmpty();
        assertGrants(UserRole.PCS_SOLICITOR, Permission.CRUD);
    }
}
