package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvictionRisksPosedPage (warrant of restitution) tests")
class EvictionRisksPosedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EvictionRisksPosedPage());
    }

    @Test
    @DisplayName("Should initialise riskDetails when null")
    void shouldInitialiseRiskDetailsWhenNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(
                EnforcementOrder.builder()
                    .warrantOfRestitutionDetails(
                        WarrantOfRestitutionDetails.builder()
                            .riskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                            .riskDetails(null)
                            .build()
                    )
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData()
                       .getEnforcementOrder()
                       .getWarrantOfRestitutionDetails()
                       .getRiskDetails())
            .isNotNull()
            .isInstanceOf(RiskDetails.class);
    }
}

