package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class LegalCostsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        FeeValidationService feeValidationService = new FeeValidationService();
        setPageUnderTest(new LegalCostsPage(feeValidationService));
    }

    @Test
    void shouldAcceptValidFee() {
        // Given
        BigDecimal expectedFee = BigDecimal.TEN;
        LegalCosts legalCostsObj = LegalCosts.builder()
            .areLegalCostsToBeClaimed(VerticalYesNo.YES)
            .amountOfLegalCosts(expectedFee)
            .build();


        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .warrantDetails(WarrantDetails.builder()
                                                      .legalCosts(legalCostsObj)
                                                      .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                .getLegalCosts().getAmountOfLegalCosts()).isEqualTo(expectedFee);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .warrantDetails(WarrantDetails.builder()
                                .enforcementRiskCategories(Set.of(RiskCategory.AGGRESSIVE_ANIMALS))
                                .riskDetails(EnforcementRiskDetails.builder()
                                        .enforcementDogsOrOtherAnimalsDetails(longText)
                                        .build())
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE,
                                             RiskCategory.AGGRESSIVE_ANIMALS.getText(),
                                             "6,800");

        assertThat(response.getErrors()).containsExactly(expectedError);
    }
}

