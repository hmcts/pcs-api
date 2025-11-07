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
class ProtestorGroupRiskPageTest extends BasePageTest {

    @InjectMocks
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ProtestorGroupRiskPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptValidText() {
        // Given
        String riskDetails = "Some protestor group details";
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementRiskCategories(Set.of(RiskCategory.PROTEST_GROUP_MEMBER))
                                  .riskDetails(EnforcementRiskDetails.builder()
                                                   .enforcementProtestGroupMemberDetails(riskDetails)
                                                   .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder()
                       .getRiskDetails().getEnforcementProtestGroupMemberDetails()).isEqualTo(riskDetails);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementRiskCategories(Set.of(RiskCategory.PROTEST_GROUP_MEMBER))
                                  .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement
                                                   .EnforcementRiskDetails.builder()
                                                   .enforcementProtestGroupMemberDetails(longText)
                                                   .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            expectedCharacterLimitErrorMessage(RiskCategory.PROTEST_GROUP_MEMBER));
    }
}
