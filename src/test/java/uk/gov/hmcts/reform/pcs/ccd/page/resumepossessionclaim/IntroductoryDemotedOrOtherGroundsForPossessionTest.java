package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IntroductoryDemotedOrOtherGroundsForPossessionTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
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

        setPageUnderTest(new IntroductoryDemotedOrOtherGroundsForPossession(
            textAreaValidationService
        ));
    }

    @Test
    void shouldNotShowReasonsPageIfRentArrearsGround() {
        // Given
        PCSCase caseData =
            PCSCase.builder()
              .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
              .introductoryDemotedOrOtherGrounds(
                  Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS))
              .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.NO);
    }

    @ParameterizedTest
    @MethodSource("testGroundsOtherThanRentArrearsScenarios")
    void shouldShowReasonsPageIfOtherGroundThanRentArrearsSelected(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        // Given
        PCSCase caseData =
            PCSCase.builder()
              .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
              .introductoryDemotedOrOtherGrounds(grounds)
              .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowReasonsPageWhenUserDoesntHaveGroundsForPossession() {
        // Given
        PCSCase caseData =
            PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                .introductoryDemotedOrOtherGrounds(null)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowGroundsOptionsWhenGroundsForPossessionIsYes() {
        // Given
        PCSCase caseData =
            PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(
                    IntroductoryDemotedOrOtherGroundsForPossessionTest
                        .buildIntroductoryDemotedOrOtherGrounds())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getIntroductoryDemotedOrOtherGrounds()).isNotEmpty();
    }

    @Test
    void shouldNotShowGroundsOptionsWhenGroundsForPossessionIsNo() {
        // Given
        PCSCase caseData =
            PCSCase.builder().hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getIntroductoryDemotedOrOtherGrounds()).isNull();
    }

    private static Stream<Arguments> testGroundsOtherThanRentArrearsScenarios() {
        return Stream.of(
                arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS)),
                arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL)),
                arguments(Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY)),
                arguments(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER)),
                arguments(
                        Set.of(
                                IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                                IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS)),
                arguments(
                        Set.of(
                                IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                                IntroductoryDemotedOrOtherGrounds.OTHER)));
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> buildIntroductoryDemotedOrOtherGrounds() {
        return Set.of(
                IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS,
                IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL,
                IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY,
                IntroductoryDemotedOrOtherGrounds.OTHER);
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate otherGroundDescription when provided")
        void shouldValidateOtherGroundDescriptionWhenProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .otherGroundDescription("Valid ground description")
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
            PCSCase caseData = PCSCase.builder()
                .otherGroundDescription(null)
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
            
            PCSCase caseData = PCSCase.builder()
                .otherGroundDescription(longText)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNotNull();
            assertThat(response.getErrors()).isNotEmpty();
        }
    }
}
