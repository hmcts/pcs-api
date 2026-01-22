package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsGroundsOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoRentArrearsGroundsForPossessionOptionsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoRentArrearsGroundsForPossessionOptions());
    }

    @Test
    void shouldPreserveSelectedMandatoryAndDiscretionaryGrounds() {
        // Given: Mandatory and Discretionary are set
        Set<AssuredMandatoryGrounds> expectedMandatory = Set.of(
            AssuredMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A,
            AssuredMandatoryGrounds.DEATH_OF_TENANT_GROUND7,
            AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8
        );
        Set<AssuredDiscretionaryGrounds> expectedDiscretionary = Set.of(
            AssuredDiscretionaryGrounds.DOMESTIC_VIOLENCE_GROUND14A,
            AssuredDiscretionaryGrounds.EMPLOYEE_LANDLORD_GROUND16,
            AssuredDiscretionaryGrounds.FALSE_STATEMENT_GROUND17
        );

        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(expectedMandatory)
                    .discretionaryGrounds(expectedDiscretionary)
                    .build()
            )
            .build();

        // When: Mid event is executed
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: Mandatory and Discretionary enum should exist in each set
        PCSCase updated = response.getData();
        assertThat(updated.getNoRentArrearsGroundsOptions().getMandatoryGrounds())
            .containsExactlyInAnyOrderElementsOf(expectedMandatory);
        assertThat(updated.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds())
            .containsExactlyInAnyOrderElementsOf(expectedDiscretionary);
    }

    @Test
    void shouldMapSelectedGroundsToEnums() {
        // Given: Mandatory and Discretionary are set
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        Set<AssuredMandatoryGrounds> expectedMandatory = Set.of(
            AssuredMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A,
            AssuredMandatoryGrounds.DEATH_OF_TENANT_GROUND7,
            AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8
        );
        Set<AssuredDiscretionaryGrounds> expectedDiscretionary = Set.of(
            AssuredDiscretionaryGrounds.DOMESTIC_VIOLENCE_GROUND14A,
            AssuredDiscretionaryGrounds.EMPLOYEE_LANDLORD_GROUND16,
            AssuredDiscretionaryGrounds.FALSE_STATEMENT_GROUND17
        );
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(expectedMandatory)
                    .discretionaryGrounds(expectedDiscretionary)
                    .build()
            )
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);

        // Then: Mandatory and Discretionary enum should exist in each set
        Set<AssuredMandatoryGrounds> selectedMandatory =
            caseDetails.getData().getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGrounds> selectedDiscretionary =
            caseDetails.getData().getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();

        assertThat(selectedMandatory).containsExactlyInAnyOrderElementsOf(expectedMandatory);
        assertThat(selectedDiscretionary).containsExactlyInAnyOrderElementsOf(expectedDiscretionary);
    }

    @ParameterizedTest
    @MethodSource("provideRentArrearsScenarios")
    void shouldSetCorrectShowFlagForNoRentArrearsReasonsPage(
        Set<AssuredMandatoryGrounds> mandatoryGrounds,
        Set<AssuredDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expectedShowFlag) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(mandatoryGrounds)
                    .discretionaryGrounds(discretionaryGrounds)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        assertThat(updatedCaseData.getNoRentArrearsGroundsOptions().getShowGroundReasonPage())
            .isEqualTo(expectedShowFlag);
    }

    private static Stream<Arguments> provideRentArrearsScenarios() {
        return Stream.of(
            Arguments.of(Set.of(AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(),
                         Set.of(
                             AssuredDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                             AssuredDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11),
                         YesOrNo.NO),
            Arguments.of(Set.of(AssuredMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A),
                         Set.of(),
                         YesOrNo.YES),
            Arguments.of(Set.of(),
                         Set.of(AssuredDiscretionaryGrounds.FALSE_STATEMENT_GROUND17),
                         YesOrNo.YES)
        );
    }
}
