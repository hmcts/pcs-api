package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NoRentArrearsGroundsForPossessionOptionsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoRentArrearsGroundsForPossessionOptions());
    }

    @ParameterizedTest
    @MethodSource("provideRentArrearsScenarios")
    void shouldSetCorrectShowFlagForNoRentArrearsReasonsPage(
        Set<NoRentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<NoRentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expectedShowFlag) {
        // Given
        PCSCase caseData = PCSCase.builder()

            .noRentArrearsMandatoryGroundsOptions(mandatoryGrounds)
            .noRentArrearsDiscretionaryGroundsOptions(discretionaryGrounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        assertThat(updatedCaseData.getShowNoRentArrearsGroundReasonPage()).isEqualTo(expectedShowFlag);
    }

    private static Stream<Arguments> provideRentArrearsScenarios() {
        return Stream.of(
            Arguments.of(Set.of(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(),
                         Set.of(NoRentArrearsDiscretionaryGrounds.RENT_ARREARS,
                                NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY),
                         YesOrNo.NO),
            Arguments.of(Set.of(NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR),
                         Set.of(),
                         YesOrNo.YES),
            Arguments.of(Set.of(),
                         Set.of(NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT),
                         YesOrNo.YES)
        );
    }
}
