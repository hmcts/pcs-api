package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class PoliceOrSocialServicesRiskPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new PoliceOrSocialServicesRiskPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptValidText() {
        // Given
        String riskDetails = "Some police or social details";
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .warrantDetails(WarrantDetails.builder()
                                .riskCategories(Set.of(RiskCategory.AGENCY_VISITS))
                                .riskDetails(EnforcementRiskDetails.builder()
                                        .policeSocialServicesDetails(riskDetails)
                                        .build())
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                       .getRiskDetails().getPoliceSocialServicesDetails()).isEqualTo(riskDetails);

    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .warrantDetails(WarrantDetails.builder()
                                .riskCategories(Set.of(RiskCategory.AGENCY_VISITS))
                                .riskDetails(EnforcementRiskDetails.builder()
                                        .policeSocialServicesDetails(longText)
                                        .build())
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE,
                                             RiskCategory.AGENCY_VISITS.getText(),
                                             "6,800");

        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);

    }
}

