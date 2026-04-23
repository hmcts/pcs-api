package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AssuredNoArrearsGroundsForPossessionPageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateSingleTextArea(any(), any(), anyInt());
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), any());

        setPageUnderTest(new AssuredNoArrearsGroundsForPossessionPage(textAreaValidationService));
    }

    @Test
    void shouldPreserveSelectedMandatoryAndDiscretionaryGrounds() {
        // Given: Mandatory and Discretionary are set
        Set<AssuredMandatoryGround> expectedMandatory = Set.of(
            AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A,
            AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7,
            AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8
        );
        Set<AssuredDiscretionaryGround> expectedDiscretionary = Set.of(
            AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A,
            AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16,
            AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
        );

        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(expectedMandatory)
                    .discretionaryGrounds(expectedDiscretionary)
                    .otherGround(Set.of())
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
        Set<AssuredMandatoryGround> expectedMandatory = Set.of(
            AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A,
            AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7,
            AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8
        );
        Set<AssuredDiscretionaryGround> expectedDiscretionary = Set.of(
            AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A,
            AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16,
            AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
        );
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(expectedMandatory)
                    .discretionaryGrounds(expectedDiscretionary)
                    .otherGround(Set.of())
                    .build()
            )
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);

        // Then: Mandatory and Discretionary enum should exist in each set
        Set<AssuredMandatoryGround> selectedMandatory =
            caseDetails.getData().getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> selectedDiscretionary =
            caseDetails.getData().getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();

        assertThat(selectedMandatory).containsExactlyInAnyOrderElementsOf(expectedMandatory);
        assertThat(selectedDiscretionary).containsExactlyInAnyOrderElementsOf(expectedDiscretionary);
    }

    @ParameterizedTest
    @MethodSource("provideRentArrearsScenarios")
    void shouldSetCorrectShowFlagForNoRentArrearsReasonsPage(
        Set<AssuredMandatoryGround> mandatoryGrounds,
        Set<AssuredDiscretionaryGround> discretionaryGrounds,
        YesOrNo expectedShowFlag) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(mandatoryGrounds)
                    .discretionaryGrounds(discretionaryGrounds)
                    .otherGround(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        assertThat(updatedCaseData.getNoRentArrearsGroundsOptions().getShowGroundReasonPage())
            .isEqualTo(expectedShowFlag);
    }

    @Test
    void shouldErrorWhenNoAdditionalGroundsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .noRentArrearsGroundsOptions(
                    AssuredNoArrearsPossessionGrounds.builder()
                        .mandatoryGrounds(Set.of())
                        .discretionaryGrounds(Set.of())
                        .otherGround(Set.of())
                        .build()
                )
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("Please select at least one ground");
    }

    @Test
    @DisplayName("Should validate otherGroundDescription when provided")
    void shouldValidateOtherGroundDescriptionWhenProvided() {
        // Given
        AssuredNoArrearsPossessionGrounds assuredNoArrearsPossessionGrounds =
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(Set.of())
                    .discretionaryGrounds(Set.of())
                    .otherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                    .otherGroundDescription("Valid ground description")
                    .build();

        PCSCase caseData = PCSCase.builder()
                .noRentArrearsGroundsOptions(assuredNoArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null otherGroundDescription gracefully")
    void shouldHandleNullOtherGroundDescriptionGracefully() {
        // Given
        AssuredNoArrearsPossessionGrounds assuredNoArrearsPossessionGrounds =
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(Set.of())
                    .discretionaryGrounds(Set.of())
                    .otherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                    .otherGroundDescription(null)
                    .build();

        PCSCase caseData = PCSCase.builder()
                .noRentArrearsGroundsOptions(assuredNoArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should return validation errors when otherGroundDescription exceeds limit")
    void shouldReturnValidationErrorsWhenOtherGroundDescriptionExceedsLimit() {
        // Given
        String longText = "a".repeat(501); // Exceeds MEDIUM_TEXT_LIMIT (500)
        List<String> validationErrors = List.of("Error message");

        lenient().doReturn(validationErrors).when(textAreaValidationService)
                .validateSingleTextArea(any(), any(), anyInt());

        AssuredNoArrearsPossessionGrounds assuredNoArrearsPossessionGrounds =
                AssuredNoArrearsPossessionGrounds.builder()
                    .otherGround(Set.of(AssuredAdditionalOtherGround.OTHER))
                    .otherGroundDescription(longText)
                    .build();

        PCSCase caseData = PCSCase.builder()
                .noRentArrearsGroundsOptions(assuredNoArrearsPossessionGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).isNotEmpty();
    }

    private static Stream<Arguments> provideRentArrearsScenarios() {
        return Stream.of(
            Arguments.of(Set.of(AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(),
                         Set.of(
                             AssuredDiscretionaryGround.RENT_ARREARS_GROUND10,
                             AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11),
                         YesOrNo.NO),
            Arguments.of(Set.of(AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A),
                         Set.of(),
                         YesOrNo.YES),
            Arguments.of(Set.of(),
                         Set.of(AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17),
                         YesOrNo.YES)
        );
    }
}
