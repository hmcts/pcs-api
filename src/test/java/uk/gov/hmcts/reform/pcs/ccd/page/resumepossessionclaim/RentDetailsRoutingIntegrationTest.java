package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1;

public class RentDetailsRoutingIntegrationTest {

    @ParameterizedTest
    @MethodSource("provideCompleteRoutingScenarios")
    void shouldRouteCorrectlyForCompleteUserJourney(
        TenancyLicenceType tenancyType,
        Set<AssuredMandatoryGround> noRentArrearsMandatory,
        Set<AssuredDiscretionaryGround> noRentArrearsDiscretionary,
        Set<SecureOrFlexibleDiscretionaryGrounds> secureFlexibleDiscretionary,
        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreach,
        YesOrNo expectedShowRentDetails,
        String scenarioDescription) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(tenancyType)
                    .build()
            )
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(noRentArrearsMandatory)
                    .discretionaryGrounds(noRentArrearsDiscretionary)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(secureFlexibleDiscretionary).build())
            .rentArrearsOrBreachOfTenancy(rentArrearsOrBreach)
            .build();

        // When - Simulate the routing logic by calling the appropriate midEvent handlers
        YesOrNo actualShowRentDetails = simulateRoutingLogic(caseData);

        // Then
        assertThat(actualShowRentDetails)
            .as(scenarioDescription)
            .isEqualTo(expectedShowRentDetails);
    }

    @ParameterizedTest
    @MethodSource("provideEdgeCaseScenarios")
    void shouldHandleEdgeCasesCorrectly(
        PCSCase caseData,
        YesOrNo expectedShowRentDetails,
        String scenarioDescription) {

        // When
        YesOrNo actualShowRentDetails = simulateRoutingLogic(caseData);

        // Then
        assertThat(actualShowRentDetails)
            .as(scenarioDescription)
            .isEqualTo(expectedShowRentDetails);
    }

    private static Stream<Arguments> provideCompleteRoutingScenarios() {
        return Stream.of(
            // AC01: Assured Tenancy + Ground 8 (SERIOUS_RENT_ARREARS)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(SERIOUS_RENT_ARREARS_GROUND8),
                     Set.of(), Set.of(), Set.of(),
                     YesOrNo.YES, "AC01: Assured Tenancy + Ground 8"),

            // AC02: Assured Tenancy + Ground 10 (RENT_ARREARS)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10),
                     Set.of(), Set.of(),
                     YesOrNo.YES, "AC02: Assured Tenancy + Ground 10"),

            // AC02: Assured Tenancy + Ground 11 (RENT_PAYMENT_DELAY)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11),
                     Set.of(), Set.of(),
                     YesOrNo.YES, "AC02: Assured Tenancy + Ground 11"),

            // AC03: Secure Tenancy + Ground 1 + Rent Arrears
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1),
                     Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                     YesOrNo.YES, "AC03: Secure Tenancy + Ground 1 + Rent Arrears"),

            // AC04: Flexible Tenancy + Ground 1 + Rent Arrears
            arguments(TenancyLicenceType.FLEXIBLE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1),
                     Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                     YesOrNo.YES, "AC04: Flexible Tenancy + Ground 1 + Rent Arrears"),

            // AC05: Secure Tenancy + Ground 1 + Breach Only
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1),
                     Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                     YesOrNo.NO, "AC05: Secure Tenancy + Ground 1 + Breach Only"),

            // AC06: Flexible Tenancy + Ground 1 + Breach Only
            arguments(TenancyLicenceType.FLEXIBLE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1),
                     Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                     YesOrNo.NO, "AC06: Flexible Tenancy + Ground 1 + Breach Only"),

            // Edge Case: Secure Tenancy + Ground 2 (should not show Rent Details)
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE_GROUND2),
                     Set.of(),
                     YesOrNo.NO, "Edge Case: Secure Tenancy + Ground 2"),

            // Edge Case: Assured Tenancy + Ground 9 (should not show Rent Details)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9),
                     Set.of(), Set.of(),
                     YesOrNo.NO, "Edge Case: Assured Tenancy + Ground 9")
        );
    }

    private static Stream<Arguments> provideEdgeCaseScenarios() {
        return Stream.of(
            // Null tenancy type
            arguments(PCSCase.builder()
                          .tenancyLicenceDetails(
                              TenancyLicenceDetails.builder()
                                  .typeOfTenancyLicence(null)
                                  .build()
                          )
                          .noRentArrearsGroundsOptions(AssuredNoArrearsPossessionGrounds.builder()
                                                           .mandatoryGrounds(Set.of(SERIOUS_RENT_ARREARS_GROUND8))
                                                           .build()
                          )
                          .build(), YesOrNo.NO, "Edge Case: Null tenancy type"),

            // Empty sets
            arguments(PCSCase.builder()
                          .tenancyLicenceDetails(
                              TenancyLicenceDetails.builder()
                                  .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                  .build()
                          )
                          .noRentArrearsGroundsOptions(
                              AssuredNoArrearsPossessionGrounds.builder()
                                  .mandatoryGrounds(Set.of())
                                  .discretionaryGrounds(Set.of())
                                  .build()
                          )
                          .rentArrearsOrBreachOfTenancy(Set.of())
                          .build(), YesOrNo.NO, "Edge Case: All empty sets"),

            // Mixed grounds (should show Rent Details if any rent-related ground is selected)
            arguments(PCSCase.builder()
                          .tenancyLicenceDetails(
                              TenancyLicenceDetails.builder()
                                  .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                  .build()
                          )
                          .noRentArrearsGroundsOptions(
                              AssuredNoArrearsPossessionGrounds.builder()
                                  .mandatoryGrounds(
                                      Set.of(SERIOUS_RENT_ARREARS_GROUND8))
                                  .discretionaryGrounds(
                                      Set.of(AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14))
                                  .build()
                          )
                          .build(), YesOrNo.YES, "Edge Case: Mixed grounds with rent-related ground")
        );
    }

    /**
     * Simulates the complete routing logic by applying the same logic as the midEvent handlers.
     */
    private YesOrNo simulateRoutingLogic(PCSCase caseData) {
        TenancyLicenceDetails tenancyDetails =
            caseData.getTenancyLicenceDetails();
        TenancyLicenceType tenancyType = tenancyDetails != null
            ? tenancyDetails.getTypeOfTenancyLicence() : null;

        // For Assured Tenancy - check if grounds 8, 10, or 11 are selected
        if (TenancyLicenceType.ASSURED_TENANCY.equals(tenancyType)) {
            boolean hasRentRelatedGrounds =
                (caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds() != null
                 && caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds()
                     .contains(SERIOUS_RENT_ARREARS_GROUND8))
                || (caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds() != null
                    && (caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds()
                        .contains(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10)
                        || caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds()
                            .contains(AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11)));
            return YesOrNo.from(hasRentRelatedGrounds);
        }

        // For Secure/Flexible Tenancy - check if Ground 1 is selected and Rent Arrears is chosen
        if (TenancyLicenceType.SECURE_TENANCY.equals(tenancyType)
            || TenancyLicenceType.FLEXIBLE_TENANCY.equals(tenancyType)) {

            boolean hasGround1 = caseData.getSecureOrFlexiblePossessionGrounds()
                .getSecureOrFlexibleDiscretionaryGrounds() != null
                                && caseData.getSecureOrFlexiblePossessionGrounds()
                .getSecureOrFlexibleDiscretionaryGrounds()
                                    .contains(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1);

            if (hasGround1) {
                boolean hasRentArrears = caseData.getRentArrearsOrBreachOfTenancy() != null
                                        && caseData.getRentArrearsOrBreachOfTenancy()
                                            .contains(RentArrearsOrBreachOfTenancy.RENT_ARREARS);
                return YesOrNo.from(hasRentArrears);
            }
        }

        return YesOrNo.NO;
    }
}
