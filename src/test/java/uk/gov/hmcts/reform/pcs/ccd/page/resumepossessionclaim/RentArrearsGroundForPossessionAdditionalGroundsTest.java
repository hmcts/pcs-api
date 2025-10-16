package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds());
    }

    @ParameterizedTest
    @MethodSource("provideNoRentArrearsScenarios")
    void shouldSetCorrectShowFlagForReasonPage(
        Set<RentArrearsGround> rentArrearsGrounds,
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expectedShowFlag) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(rentArrearsGrounds)
            .rentArrearsMandatoryGrounds(mandatoryGrounds)
            .rentArrearsDiscretionaryGrounds(discretionaryGrounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        assertThat(updatedCaseData.getShowRentArrearsGroundReasonPage()).isEqualTo(expectedShowFlag);
    }

    private static Stream<Arguments> provideNoRentArrearsScenarios() {
        return Stream.of(
            Arguments.of(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10,
                                RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                         Set.of(),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10,
                                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                         Set.of(),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(),
                         Set.of(RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6),
                         Set.of(),
                         YesOrNo.YES),
            Arguments.of(Set.of(),
                         Set.of(),
                         Set.of(RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12),
                         YesOrNo.YES)
        );
    }
}
