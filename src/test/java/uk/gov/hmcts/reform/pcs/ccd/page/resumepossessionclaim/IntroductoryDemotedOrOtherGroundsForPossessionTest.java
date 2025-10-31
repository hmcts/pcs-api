package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntroductoryDemotedOrOtherGroundsForPossession Integration Tests")
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
        
        setPageUnderTest(new IntroductoryDemotedOrOtherGroundsForPossession(textAreaValidationService));
    }

    @Nested
    @DisplayName("Text Area Validation Tests")
    class TextAreaValidationTests {

        @Test
        @DisplayName("Should validate otherGroundDescription when provided")
        void shouldValidateOtherGroundDescriptionWhenProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER))
                .otherGroundDescription("Valid description under 250 characters")
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
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL))
                .otherGroundDescription(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty otherGroundDescription gracefully")
        void shouldHandleEmptyOtherGroundDescriptionGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER))
                .otherGroundDescription("")
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should not show reasons page if only rent arrears ground is selected")
        void shouldNotShowReasonsPageIfOnlyRentArrearsGround() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS))
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getShowIntroductoryDemotedOtherGroundReasonPage())
                .isEqualTo(YesOrNo.NO);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should show reasons page if other ground than rent arrears is selected")
        void shouldShowReasonsPageIfOtherGroundThanRentArrearsSelected() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL))
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getShowIntroductoryDemotedOtherGroundReasonPage())
                .isEqualTo(YesOrNo.YES);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should show reasons page when user doesn't have grounds for possession")
        void shouldShowReasonsPageWhenUserDoesntHaveGroundsForPossession() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO)
                .introductoryDemotedOrOtherGrounds(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getShowIntroductoryDemotedOtherGroundReasonPage())
                .isEqualTo(YesOrNo.YES);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should show reasons page if rent arrears and other ground are selected")
        void shouldShowReasonsPageIfRentArrearsAndOtherGroundSelected() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(
                    IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                    IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL
                ))
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getShowIntroductoryDemotedOtherGroundReasonPage())
                .isEqualTo(YesOrNo.YES);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should show reasons page when OTHER ground is selected with otherGroundDescription")
        void shouldShowReasonsPageWhenOtherGroundIsSelectedWithOtherGroundDescription() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER))
                .otherGroundDescription("Some other ground description")
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getShowIntroductoryDemotedOtherGroundReasonPage())
                .isEqualTo(YesOrNo.YES);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}

