package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NoRentArrearsGroundsForPossessionOptionsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoRentArrearsGroundsForPossessionOptions());
    }

    @Test
    void shouldMapSelectedGroundsToEnums() {
        // Given: Mandatory and Discretionary are set
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        Set<NoRentArrearsMandatoryGrounds> expectedMandatory = Set.of(
            NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR,
            NoRentArrearsMandatoryGrounds.DEATH_OF_TENANT,
            NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS);
        Set<NoRentArrearsDiscretionaryGrounds> expectedDiscretionary = Set.of(
            NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE,
            NoRentArrearsDiscretionaryGrounds.LANDLORD_EMPLOYEE,
            NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT);
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsDiscretionaryGroundsOptions(expectedDiscretionary)
            .noRentArrearsMandatoryGroundsOptions(expectedMandatory)
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);

        // Then: Mandatory and Discretionary enum should exist in each set
        Set<NoRentArrearsMandatoryGrounds> selectedMandatory =
            caseDetails.getData().getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> selectedDiscretionary =
            caseDetails.getData().getNoRentArrearsDiscretionaryGroundsOptions();

        assertThat(selectedMandatory).containsExactlyInAnyOrderElementsOf(expectedMandatory);
        assertThat(selectedDiscretionary).containsExactlyInAnyOrderElementsOf(expectedDiscretionary);
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
