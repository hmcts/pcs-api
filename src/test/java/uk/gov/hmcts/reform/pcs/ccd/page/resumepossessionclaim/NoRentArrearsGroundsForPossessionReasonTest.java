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
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

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
    private TextAreaValidationService textAreaValidationService;

    private NoRentArrearsGroundsForPossessionReason pageUnderTest;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        pageUnderTest = new NoRentArrearsGroundsForPossessionReason(textAreaValidationService);
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
                    .ownerOccupier("Owner occupier reason")
                    .repossessionByLender("Repossession reason")
                    .holidayLet("Holiday let reason")
                    .studentLet("Student let reason")
                    .ministerOfReligion("Minister reason")
                    .redevelopment("Redevelopment reason")
                    .deathOfTenant("Death of tenant reason")
                    .antisocialBehaviour("Antisocial behaviour reason")
                    .noRightToRent("No right to rent reason")
                    .suitableAlternativeAccomodation("Suitable accommodation reason")
                    .breachOfTenancyConditions("Breach of tenancy reason")
                    .propertyDeterioration("Property deterioration reason")
                    .nuisanceOrIllegalUse("Nuisance reason")
                    .domesticViolence("Domestic violence reason")
                    .offenceDuringRiot("Offence during riot reason")
                    .furnitureDeterioration("Furniture deterioration reason")
                    .landlordEmployee("Landlord employee reason")
                    .falseStatement("False statement reason")
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
                    .ownerOccupier("Only owner occupier reason")
                    .repossessionByLender("Only repossession reason")
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
                    .ownerOccupier("Owner occupier")
                    .holidayLet("Holiday let")
                    .deathOfTenant("Death of tenant")
                    .antisocialBehaviour("Antisocial behaviour")
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
                    .suitableAlternativeAccomodation("Suitable accommodation")
                    .breachOfTenancyConditions("Breach of tenancy")
                    .propertyDeterioration("Property deterioration")
                    .nuisanceOrIllegalUse("Nuisance")
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

