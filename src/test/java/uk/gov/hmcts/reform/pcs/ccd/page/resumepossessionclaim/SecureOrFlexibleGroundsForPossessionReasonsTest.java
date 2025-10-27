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
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecureOrFlexibleGroundsForPossessionReasons Integration Tests")
class SecureOrFlexibleGroundsForPossessionReasonsTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());
        
        setPageUnderTest(new SecureOrFlexibleGroundsForPossessionReasons(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate all text area fields when secure or flexible grounds reasons are provided")
        void shouldValidateAllTextAreaFieldsWhenSecureOrFlexibleGroundsReasonsAreProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleGroundsReasons(SecureOrFlexibleGroundsReasons.builder()
                    .breachOfTenancyGround("Breach of tenancy reason")
                    .nuisanceOrImmoralUseGround("Nuisance reason")
                    .domesticViolenceGround("Domestic violence reason")
                    .propertyDeteriorationGround("Property deterioration reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null secure or flexible grounds reasons gracefully")
        void shouldHandleNullSecureOrFlexibleGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleGroundsReasons(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty secure or flexible grounds reasons gracefully")
        void shouldHandleEmptySecureOrFlexibleGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleGroundsReasons(SecureOrFlexibleGroundsReasons.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle partial secure or flexible grounds reasons gracefully")
        void shouldHandlePartialSecureOrFlexibleGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleGroundsReasons(SecureOrFlexibleGroundsReasons.builder()
                    .breachOfTenancyGround("Only breach of tenancy reason")
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

