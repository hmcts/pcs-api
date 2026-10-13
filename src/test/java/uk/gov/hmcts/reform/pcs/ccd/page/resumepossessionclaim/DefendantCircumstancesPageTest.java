package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefendantCircumstancesPage Integration Tests")
class DefendantCircumstancesPageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateSingleTextArea(any(), any(), anyInt());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new DefendantCircumstancesPage(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate defendant circumstances text area when provided")
        void shouldValidateDefendantCircumstancesTextAreaWhenProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .defendantCircumstances(DefendantCircumstances.builder()
                    .hasSingleDefendantCircumstancesInfo(VerticalYesNo.YES)
                    .singleDefendantCircumstancesInfo("Defendant circumstances details")
                    .defendantTermPossessive("tenant’s")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should save the answers from the one-defendant fields")
        void shouldSaveAnswersFromSingleDefendantFields() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .addAnotherDefendant(VerticalYesNo.NO)
                .defendantCircumstances(DefendantCircumstances.builder()
                    .hasSingleDefendantCircumstancesInfo(VerticalYesNo.YES)
                    .singleDefendantCircumstancesInfo("One defendant details")
                    .hasMultipleDefendantsCircumstancesInfo(VerticalYesNo.NO)
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            DefendantCircumstances result = response.getData().getDefendantCircumstances();
            assertThat(result.getHasDefendantCircumstancesInfo()).isEqualTo(VerticalYesNo.YES);
            assertThat(result.getDefendantCircumstancesInfo()).isEqualTo("One defendant details");
            verify(textAreaValidationService).validateSingleTextArea(
                "One defendant details", "Give details about the defendant’s circumstances",
                TextAreaValidationService.LONG_TEXT_LIMIT);
        }

        @Test
        @DisplayName("Should save the answers from the several-defendants fields")
        void shouldSaveAnswersFromMultipleDefendantsFields() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .addAnotherDefendant(VerticalYesNo.YES)
                .defendantCircumstances(DefendantCircumstances.builder()
                    .hasSingleDefendantCircumstancesInfo(VerticalYesNo.NO)
                    .hasMultipleDefendantsCircumstancesInfo(VerticalYesNo.YES)
                    .multipleDefendantsCircumstancesInfo("Several defendants details")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            DefendantCircumstances result = response.getData().getDefendantCircumstances();
            assertThat(result.getHasDefendantCircumstancesInfo()).isEqualTo(VerticalYesNo.YES);
            assertThat(result.getDefendantCircumstancesInfo()).isEqualTo("Several defendants details");
            verify(textAreaValidationService).validateSingleTextArea(
                "Several defendants details", "Give details about the defendants’ circumstances",
                TextAreaValidationService.LONG_TEXT_LIMIT);
        }

        @Test
        @DisplayName("Should handle null defendant circumstances gracefully")
        void shouldHandleNullDefendantCircumstancesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .defendantCircumstances(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null defendant circumstances info gracefully")
        void shouldHandleNullDefendantCircumstancesInfoGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .defendantCircumstances(DefendantCircumstances.builder()
                    .hasSingleDefendantCircumstancesInfo(VerticalYesNo.YES)
                    .singleDefendantCircumstancesInfo(null)
                    .defendantTermPossessive("tenant’s")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty defendant circumstances gracefully")
        void shouldHandleEmptyDefendantCircumstancesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .defendantCircumstances(DefendantCircumstances.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}

