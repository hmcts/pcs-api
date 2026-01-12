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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediationAndSettlement Integration Tests")
class MediationAndSettlementTest extends BasePageTest {

    @Mock
    private TextValidationService textValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new MediationAndSettlement(textValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate mediation and settlement text areas")
        void shouldValidateMediationAndSettlementTextAreas() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .mediationAttempted(VerticalYesNo.YES)
                .mediationAttemptedDetails("Mediation was attempted but failed")
                .settlementAttempted(VerticalYesNo.YES)
                .settlementAttemptedDetails("Settlement discussions took place")
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null text area values gracefully")
        void shouldHandleNullTextAreaValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .mediationAttempted(VerticalYesNo.YES)
                .mediationAttemptedDetails(null)
                .settlementAttempted(VerticalYesNo.YES)
                .settlementAttemptedDetails(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty case data gracefully")
        void shouldHandleEmptyCaseDataGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder().build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}
