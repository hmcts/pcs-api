package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.BreathingSpaceNotSureContent;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BreathingSpaceNotSurePage tests")
class BreathingSpaceNotSurePageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new BreathingSpaceNotSurePage());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        @Test
        @DisplayName("Should return error to block progression when breathing space status is not sure")
        void shouldReturnErrorWhenBreathingSpaceNotSure() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response =
                callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrorMessageOverride())
                .isEqualTo(BreathingSpaceNotSureContent.ERROR_MESSAGE);
        }
    }

}
