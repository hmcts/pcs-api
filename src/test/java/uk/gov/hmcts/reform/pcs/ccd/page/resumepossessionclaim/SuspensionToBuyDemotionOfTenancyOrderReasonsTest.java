package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspensionToBuyDemotionOfTenancyOrderReasons Integration Tests")
class SuspensionToBuyDemotionOfTenancyOrderReasonsTest extends BasePageTest {

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
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());
        
        setPageUnderTest(new SuspensionToBuyDemotionOfTenancyOrderReasons(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate suspension order reason and demotion order reason when provided")
        void shouldValidateSuspensionOrderReasonAndDemotionOrderReasonWhenProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder()
                    .suspensionOrderReason("Suspension order reason")
                    .demotionOrderReason("Demotion order reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
            verify(textAreaValidationService).validateSingleTextArea(
                eq("Suspension order reason"),
                any(),
                anyInt()
            );
            verify(textAreaValidationService).validateSingleTextArea(
                eq("Demotion order reason"),
                any(),
                anyInt()
            );
        }

        @Test
        @DisplayName("Should handle null suspension of right to buy demotion of tenancy gracefully")
        void shouldHandleNullSuspensionOfRightToBuyDemotionOfTenancyGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .suspensionOfRightToBuyDemotionOfTenancy(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null suspension order reason gracefully")
        void shouldHandleNullSuspensionOrderReasonGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder()
                    .suspensionOrderReason(null)
                    .demotionOrderReason("Demotion order reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null demotion order reason gracefully")
        void shouldHandleNullDemotionOrderReasonGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder()
                    .suspensionOrderReason("Suspension order reason")
                    .demotionOrderReason(null)
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should return validation errors when text exceeds limit")
        void shouldReturnValidationErrorsWhenTextExceedsLimit() {
            // Given
            String longText = "a".repeat(251); // Exceeds SHORT_TEXT_LIMIT (250)
            List<String> validationErrors = List.of("Error message");
            
            lenient().doReturn(validationErrors).when(textAreaValidationService)
                .validateSingleTextArea(any(), any(), anyInt());
            
            PCSCase caseData = PCSCase.builder()
                .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder()
                    .suspensionOrderReason(longText)
                    .demotionOrderReason("Valid reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNotNull();
            assertThat(response.getErrors()).isNotEmpty();
        }
    }
}

