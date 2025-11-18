package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
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
@DisplayName("ASBQuestionsWales Integration Tests")
class ASBQuestionsWalesTest extends BasePageTest {

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
        
        setPageUnderTest(new ASBQuestionsWales(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate ASBQuestionsWales text areas")
        void shouldValidateASBQuestionsWalesTextAreas() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .asbQuestionsWales(ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails("Details about antisocial behaviour")
                    .illegalPurposesUse(VerticalYesNo.YES)
                    .illegalPurposesUseDetails("Details about illegal purposes use")
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails("Details about other prohibited conduct")
                    .build())
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
                .asbQuestionsWales(ASBQuestionsDetailsWales.builder()
                    .antisocialBehaviour(VerticalYesNo.YES)
                    .antisocialBehaviourDetails(null)
                    .illegalPurposesUse(VerticalYesNo.YES)
                    .illegalPurposesUseDetails(null)
                    .otherProhibitedConduct(VerticalYesNo.YES)
                    .otherProhibitedConductDetails(null)
                    .build())
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