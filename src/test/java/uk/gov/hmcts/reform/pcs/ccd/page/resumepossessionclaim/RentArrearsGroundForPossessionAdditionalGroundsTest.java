package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds(
            new GroundsRules(new TextAreaValidationService())));
    }

    @Test
    void shouldErrorWhenNoAdditionalGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .assuredRentArrearsPossessionGrounds(
                AssuredRentArrearsPossessionGrounds.builder()
                    .additionalMandatoryGrounds(Set.of())
                    .additionalDiscretionaryGrounds(Set.of())
                    .additionalOtherGround(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("Please select at least one ground");
    }

    @Test
    void shouldNotErrorWhenOtherGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .assuredRentArrearsPossessionGrounds(
                        AssuredRentArrearsPossessionGrounds.builder()
                                .additionalMandatoryGrounds(Set.of())
                                .additionalDiscretionaryGrounds(Set.of())
                                .additionalOtherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                                .build()
                )
                .build();


        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
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
                    .additionalOtherGround(Set.of())
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
                            Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12))
                    .additionalOtherGround(Set.of())
                    .build()
            )
            .build();


        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }
}
