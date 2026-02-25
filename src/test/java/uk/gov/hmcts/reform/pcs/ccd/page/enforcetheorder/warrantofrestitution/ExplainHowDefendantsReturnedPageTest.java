package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExplainHowDefendantsReturnedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ExplainHowDefendantsReturnedPage(new TextAreaValidationService()));
    }

    @Nested
    @DisplayName("MidEvent validation")
    class MidEventValidation {

        @Test
        void shouldNotReturnErrorsWhenTextWithinLimit() {
            WarrantOfRestitutionDetails details = WarrantOfRestitutionDetails.builder()
                    .howDefendantsReturned("The defendants returned the following week.")
                    .build();
            EnforcementOrder order = EnforcementOrder.builder()
                    .selectEnforcementType(SelectEnforcementType.WARRANT_OF_RESTITUTION)
                    .warrantOfRestitutionDetails(details)
                    .build();
            PCSCase caseData = PCSCase.builder()
                    .enforcementOrder(order)
                    .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrorMessageOverride()).isNull();
            assertThat(response.getData()).isEqualTo(caseData);
        }

        @Test
        void shouldReturnErrorWhenTextExceedsCharacterLimit() {
            int limit = 6800;
            String overLimit = "a".repeat(limit + 1);
            WarrantOfRestitutionDetails details = WarrantOfRestitutionDetails.builder()
                    .howDefendantsReturned(overLimit)
                    .build();
            EnforcementOrder order = EnforcementOrder.builder()
                    .selectEnforcementType(SelectEnforcementType.WARRANT_OF_RESTITUTION)
                    .warrantOfRestitutionDetails(details)
                    .build();
            PCSCase caseData = PCSCase.builder()
                    .enforcementOrder(order)
                    .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrorMessageOverride())
                    .isNotNull()
                    .contains("How did the defendants return to the property?")
                    .contains("6,800");
        }
    }
}
