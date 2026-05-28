package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class MustApplyForHelpWithFeesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new MustApplyForHelpWithFees());
    }

    @Test
    void shouldAlwaysReturnErrorFromMidEvent() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isEqualTo("You cannot continue until you have their reference number for Help with Fees");
    }

}
