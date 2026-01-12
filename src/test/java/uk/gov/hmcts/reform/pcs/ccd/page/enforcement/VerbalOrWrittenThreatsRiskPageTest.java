package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class VerbalOrWrittenThreatsRiskPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextValidationService textValidationService = new TextValidationService();
        setPageUnderTest(new VerbalOrWrittenThreatsRiskPage(textValidationService));
    }

    @Test
    void shouldAcceptValidText() {
        // Given
        String riskDetails = "Some verbal details";
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementRiskCategories(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                                  .riskDetails(EnforcementRiskDetails
                                                   .builder()
                                                   .enforcementVerbalOrWrittenThreatsDetails(riskDetails)
                                                   .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder()
                       .getRiskDetails().getEnforcementVerbalOrWrittenThreatsDetails()).isEqualTo(riskDetails);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementRiskCategories(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                                  .riskDetails(EnforcementRiskDetails
                                                   .builder()
                                                   .enforcementVerbalOrWrittenThreatsDetails(longText)
                                                   .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE,
                                             RiskCategory.VERBAL_OR_WRITTEN_THREATS.getText(),
                                             "6,800");

        assertThat(response.getErrors()).containsExactly(expectedError);
    }
}
