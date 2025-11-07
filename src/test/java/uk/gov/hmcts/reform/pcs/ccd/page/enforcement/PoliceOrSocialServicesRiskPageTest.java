package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcement.RiskCategoryTestUtil.expectedCharacterLimitErrorMessage;

@ExtendWith(MockitoExtension.class)
class PoliceOrSocialServicesRiskPageTest extends BasePageTest {

    @InjectMocks
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new PoliceOrSocialServicesRiskPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptValidText() {
        // Given
        String riskDetails = "Some police or social details";

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .enforcementRiskCategories(Set.of(RiskCategory.AGENCY_VISITS))
                        .riskDetails(EnforcementRiskDetails.builder()
                                .enforcementPoliceOrSocialServicesDetails(riskDetails)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder()
                       .getRiskDetails().getEnforcementPoliceOrSocialServicesDetails()).isEqualTo(riskDetails);

    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .enforcementRiskCategories(Set.of(RiskCategory.AGENCY_VISITS))
                        .riskDetails(EnforcementRiskDetails.builder()
                                .enforcementPoliceOrSocialServicesDetails(longText)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            expectedCharacterLimitErrorMessage(RiskCategory.AGENCY_VISITS));

    }
}

