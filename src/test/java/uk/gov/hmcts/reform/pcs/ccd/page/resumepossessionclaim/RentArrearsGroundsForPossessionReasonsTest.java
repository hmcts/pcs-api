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
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentArrearsGroundsForPossessionReasons Integration Tests")
class RentArrearsGroundsForPossessionReasonsTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new RentArrearsGroundsForPossessionReasons(textAreaValidationService));
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate all rent arrears grounds reasons when provided")
        void shouldValidateAllRentArrearsGroundsReasonsWhenProvided() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                    .ownerOccupierReason("Owner occupier reason")
                    .repossessionByLenderReason("Repossession by lender reason")
                    .holidayLetReason("Holiday let reason")
                    .studentLetReason("Student let reason")
                    .ministerOfReligionReason("Minister of religion reason")
                    .redevelopmentReason("Redevelopment reason")
                    .deathOfTenantReason("Death of tenant reason")
                    .antisocialBehaviourReason("Antisocial behaviour reason")
                    .noRightToRentReason("No right to rent reason")
                    .suitableAltAccommodationReason("Suitable alternative accommodation reason")
                    .breachOfTenancyConditionsReason("Breach of tenancy conditions reason")
                    .propertyDeteriorationReason("Property deterioration reason")
                    .nuisanceAnnoyanceReason("Nuisance annoyance reason")
                    .domesticViolenceReason("Domestic violence reason")
                    .offenceDuringRiotReason("Offence during riot reason")
                    .furnitureDeteriorationReason("Furniture deterioration reason")
                    .employeeOfLandlordReason("Employee of landlord reason")
                    .tenancyByFalseStatementReason("Tenancy by false statement reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null rent arrears grounds reasons gracefully")
        void shouldHandleNullRentArrearsGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(null)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty rent arrears grounds reasons gracefully")
        void shouldHandleEmptyRentArrearsGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle partial rent arrears grounds reasons gracefully")
        void shouldHandlePartialRentArrearsGroundsReasonsGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                    .ownerOccupierReason("Only owner occupier reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should validate mandatory grounds reasons")
        void shouldValidateMandatoryGroundsReasons() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                    .ownerOccupierReason("Owner occupier reason")
                    .repossessionByLenderReason("Repossession by lender reason")
                    .holidayLetReason("Holiday let reason")
                    .studentLetReason("Student let reason")
                    .ministerOfReligionReason("Minister of religion reason")
                    .redevelopmentReason("Redevelopment reason")
                    .deathOfTenantReason("Death of tenant reason")
                    .antisocialBehaviourReason("Antisocial behaviour reason")
                    .noRightToRentReason("No right to rent reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should validate discretionary grounds reasons")
        void shouldValidateDiscretionaryGroundsReasons() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder()
                    .suitableAltAccommodationReason("Suitable alternative accommodation reason")
                    .breachOfTenancyConditionsReason("Breach of tenancy conditions reason")
                    .propertyDeteriorationReason("Property deterioration reason")
                    .nuisanceAnnoyanceReason("Nuisance annoyance reason")
                    .domesticViolenceReason("Domestic violence reason")
                    .offenceDuringRiotReason("Offence during riot reason")
                    .furnitureDeteriorationReason("Furniture deterioration reason")
                    .employeeOfLandlordReason("Employee of landlord reason")
                    .tenancyByFalseStatementReason("Tenancy by false statement reason")
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }

    @Nested
    @DisplayName("YesOrNo mapping integration tests")
    class VisibilityFlagIntegrationTests {
        @Test
        @DisplayName("Should set showOwnerOccupierReason to YES when OWNER_OCCUPIER ground selected")
        void shouldSetShowOwnerOccupierReasonWhenMandatoryGroundSelected() {
            //Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsAdditionalGrounds(
                    RentArrearsAdditionalGrounds.builder()
                        .mandatoryGrounds(Set.of(
                            RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
                        .build()
                )
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder().build())
                .build();

            //When
            AboutToStartOrSubmitResponse<PCSCase, State> response =
                callMidEventHandler(caseData);

            //Then
            assertThat(response.getData()
                           .getRentArrearsGroundsReasons()
                           .getShowOwnerOccupierReason())
                .isEqualTo(YesOrNo.YES);
        }

        @Test
        @DisplayName("Should set showBreachOfTenancyConditionsReason to YES when BREACH_TENANCY ground selected")
        void shouldSetShowBreachOfTenancyReasonWhenDiscretionaryGroundSelected() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsAdditionalGrounds(
                    RentArrearsAdditionalGrounds.builder()
                        .discretionaryGrounds(Set.of(
                            RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12))
                        .build()
                )
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder().build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response =
                callMidEventHandler(caseData);

            // Then
            assertThat(response.getData()
                           .getRentArrearsGroundsReasons()
                           .getShowBreachOfTenancyConditionsReason())
                .isEqualTo(YesOrNo.YES);
        }

        @Test
        @DisplayName("Should not fail when additional grounds are null")
        void shouldHandleNullAdditionalGrounds() {

            //Given
            PCSCase caseData = PCSCase.builder()
                .rentArrearsGroundsReasons(RentArrearsGroundsReasons.builder().build())
                .build();

            //When
            AboutToStartOrSubmitResponse<PCSCase, State> response =
                callMidEventHandler(caseData);

            //Then
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}

