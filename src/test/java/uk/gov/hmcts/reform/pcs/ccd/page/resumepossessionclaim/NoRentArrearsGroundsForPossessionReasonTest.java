package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoRentArrearsGroundsForPossessionReason Integration Tests")
class NoRentArrearsGroundsForPossessionReasonTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        doReturn(new ArrayList<>()).when(textAreaValidationService).validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());
        
        setPageUnderTest(new NoRentArrearsGroundsForPossessionReason(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate all text area fields when no rent arrears reasons are provided")
        void shouldValidateAllTextAreaFieldsWhenNoRentArrearsReasonsAreProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                    .ownerOccupierTextArea("Owner occupier reason")
                    .repossessionByLenderTextArea("Repossession reason")
                    .holidayLetTextArea("Holiday let reason")
                    .studentLetTextArea("Student let reason")
                    .ministerOfReligionTextArea("Minister reason")
                    .redevelopmentTextArea("Redevelopment reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null no rent arrears reasons gracefully")
        void shouldHandleNullNoRentArrearsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty no rent arrears reasons gracefully")
        void shouldHandleEmptyNoRentArrearsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle partial no rent arrears reasons gracefully")
        void shouldHandlePartialNoRentArrearsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                    .ownerOccupierTextArea("Only owner occupier reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}

