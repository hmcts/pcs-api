package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds());
    }

    @Test
    void shouldErrorWhenNoAdditionalGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldNotErrorWhenAdditionalMandatoryGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(
                        Set.of(AssuredAdditionalMandatoryGrounds.REDEVELOPMENT_GROUND6)
                    )
                    .additionalDiscretionaryGrounds(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldNotErrorWhenAdditionalDiscretionaryGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(
                        Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12)
                    )
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

}
