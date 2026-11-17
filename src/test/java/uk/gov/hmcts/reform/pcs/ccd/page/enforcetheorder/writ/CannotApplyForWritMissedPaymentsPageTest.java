package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.CannotApplyForWritMissedPaymentsPage.ERROR_MESSAGE;

@DisplayName("CannotApplyForWritMissedPaymentsPage tests")
class CannotApplyForWritMissedPaymentsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new CannotApplyForWritMissedPaymentsPage());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        @Test
        @DisplayName("Should always return error to block progression")
        void shouldAlwaysReturnError() {

            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .writDetails(WritDetails
                                         .builder()
                                         .defendantMissedPayments(VerticalYesNo.NO)
                                         .build())
                                      .build())
                .build();


            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrorMessageOverride()).isEqualTo(ERROR_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should only show page on the writ flow and missed payment selection is No")
    void shouldOnlyShowPageForWritWhenNoMissedPayments() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry(
                "cannotApplyForWritMissedPayments",
                "chooseEnforcementType=\"WRIT\" AND writDefendantMissedPayments=\"NO\""
            );
    }
}
