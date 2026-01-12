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
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoRentArrearsGroundsForPossessionReason Tests")
class NoRentArrearsGroundsForPossessionReasonTest extends BasePageTest {

    @Mock
    private TextValidationService textValidationService;

    private NoRentArrearsGroundsForPossessionReason pageUnderTest;

    @BeforeEach
    void setUp() {
        // Configure TextValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textValidationService)
            .validateMultipleTextAreas(any(), any());
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textValidationService).createValidationResponse(any(), anyList());

        pageUnderTest = new NoRentArrearsGroundsForPossessionReason(textValidationService);
        setPageUnderTest(pageUnderTest);
    }

    @Test
    @DisplayName("Should create page configuration successfully")
    void shouldCreatePageConfigurationSuccessfully() {
        // Given & When & Then - Just verify the page configuration is created without errors
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(NoRentArrearsGroundsForPossessionReason.class);
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
                    .deathOfTenantTextArea("Death of tenant reason")
                    .antisocialBehaviourTextArea("Antisocial behaviour reason")
                    .noRightToRentTextArea("No right to rent reason")
                    .suitableAccomTextArea("Suitable accommodation reason")
                    .breachOfTenancyConditionsTextArea("Breach of tenancy reason")
                    .propertyDeteriorationTextArea("Property deterioration reason")
                    .nuisanceOrIllegalUseTextArea("Nuisance reason")
                    .domesticViolenceTextArea("Domestic violence reason")
                    .offenceDuringRiotTextArea("Offence during riot reason")
                    .furnitureDeteriorationTextArea("Furniture deterioration reason")
                    .landlordEmployeeTextArea("Landlord employee reason")
                    .falseStatementTextArea("False statement reason")
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
                    .repossessionByLenderTextArea("Only repossession reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle mandatory grounds only")
        void shouldHandleMandatoryGroundsOnly() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                    .ownerOccupierTextArea("Owner occupier")
                    .holidayLetTextArea("Holiday let")
                    .deathOfTenantTextArea("Death of tenant")
                    .antisocialBehaviourTextArea("Antisocial behaviour")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle discretionary grounds only")
        void shouldHandleDiscretionaryGroundsOnly() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                    .suitableAccomTextArea("Suitable accommodation")
                    .breachOfTenancyConditionsTextArea("Breach of tenancy")
                    .propertyDeteriorationTextArea("Property deterioration")
                    .nuisanceOrIllegalUseTextArea("Nuisance")
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

