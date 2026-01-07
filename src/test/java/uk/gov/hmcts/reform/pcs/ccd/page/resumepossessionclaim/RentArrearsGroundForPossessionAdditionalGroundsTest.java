package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds());
    }

    @Test
    void shouldErrorWhenRentArrearsSelectedAndNoAdditionalSelected() {
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalMandatorySelected() {
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .rentArrearsAdditionalGrounds(
                RentArrearsAdditionalGrounds.builder()
                    .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
                    .assuredAdditionalDiscretionaryGrounds(Set.of(AssuredAdditionalDiscretionaryGrounds
                                                                      .BREACH_TENANCY_GROUND12))
                    .build()
            )
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1
            );
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalDiscretionarySelected() {
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .rentArrearsAdditionalGrounds(
                RentArrearsAdditionalGrounds.builder()
                    .assuredAdditionalDiscretionaryGrounds(Set.of(AssuredAdditionalDiscretionaryGrounds
                                                                      .BREACH_TENANCY_GROUND12))
                    .build()
            )
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12
            );
    }

    @ParameterizedTest
    @MethodSource("provideShowRentArrearsGroundReasonPageScenarios")
    void shouldSetCorrectShowRentArrearsGroundReasonPage(
        Set<RentArrearsGround> rentArrearsGrounds,
        Set<AssuredAdditionalMandatoryGrounds> additionalMandatory,
        Set<AssuredAdditionalDiscretionaryGrounds> additionalDiscretionary,
        YesOrNo expectedShowReasonPage) {

        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(rentArrearsGrounds)
            .rentArrearsAdditionalGrounds(
                RentArrearsAdditionalGrounds.builder()
                    .assuredAdditionalMandatoryGrounds(additionalMandatory)
                    .assuredAdditionalDiscretionaryGrounds(additionalDiscretionary)
                    .build()
            )
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        if (response.getErrors() == null || response.getErrors().isEmpty()) {
            assertThat(response.getData().getShowRentArrearsGroundReasonPage())
                .isEqualTo(expectedShowReasonPage);
        }
    }

    private static Stream<Arguments> provideShowRentArrearsGroundReasonPageScenarios() {
        return Stream.of(
            Arguments.of(
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(),
                Set.of(),
                YesOrNo.NO
            ),
            Arguments.of(
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1),
                Set.of(),
                YesOrNo.YES
            ),
            Arguments.of(
                Set.of(RentArrearsGround.RENT_ARREARS_GROUND10),
                Set.of(),
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12),
                YesOrNo.YES
            ),
            Arguments.of(
                Set.of(),
                Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1),
                Set.of(),
                YesOrNo.YES
            ),
            Arguments.of(
                Set.of(),
                Set.of(),
                Set.of(),
                YesOrNo.NO
            )
        );
    }
}
