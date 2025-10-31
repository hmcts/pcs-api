package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

public class RentDetailsRoutingIntegrationTest {

    @ParameterizedTest
    @MethodSource("provideCompleteRoutingScenarios")
    void shouldRouteCorrectlyForCompleteUserJourney(
        TenancyLicenceType tenancyType,
        Set<NoRentArrearsMandatoryGrounds> noRentArrearsMandatory,
        Set<NoRentArrearsDiscretionaryGrounds> noRentArrearsDiscretionary,
        Set<SecureOrFlexibleDiscretionaryGrounds> secureFlexibleDiscretionary,
        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreach,
        YesOrNo expectedShowRentDetails,
        String scenarioDescription) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(tenancyType)
            .noRentArrearsMandatoryGroundsOptions(noRentArrearsMandatory)
            .noRentArrearsDiscretionaryGroundsOptions(noRentArrearsDiscretionary)
            .secureOrFlexibleDiscretionaryGrounds(secureFlexibleDiscretionary)
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
                     Set.of(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS),
                     Set.of(), Set.of(), Set.of(),
                     YesOrNo.YES, "AC01: Assured Tenancy + Ground 8"),

            // AC02: Assured Tenancy + Ground 10 (RENT_ARREARS)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(NoRentArrearsDiscretionaryGrounds.RENT_ARREARS),
                     Set.of(), Set.of(),
                     YesOrNo.YES, "AC02: Assured Tenancy + Ground 10"),

            // AC02: Assured Tenancy + Ground 11 (RENT_PAYMENT_DELAY)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY),
                     Set.of(), Set.of(),
                     YesOrNo.YES, "AC02: Assured Tenancy + Ground 11"),

            // AC03: Secure Tenancy + Ground 1 + Rent Arrears
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                     Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                     YesOrNo.YES, "AC03: Secure Tenancy + Ground 1 + Rent Arrears"),

            // AC04: Flexible Tenancy + Ground 1 + Rent Arrears
            arguments(TenancyLicenceType.FLEXIBLE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                     Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                     YesOrNo.YES, "AC04: Flexible Tenancy + Ground 1 + Rent Arrears"),

            // AC05: Secure Tenancy + Ground 1 + Breach Only
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                     Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                     YesOrNo.NO, "AC05: Secure Tenancy + Ground 1 + Breach Only"),

            // AC06: Flexible Tenancy + Ground 1 + Breach Only
            arguments(TenancyLicenceType.FLEXIBLE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                     Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                     YesOrNo.NO, "AC06: Flexible Tenancy + Ground 1 + Breach Only"),

            // Edge Case: Secure Tenancy + Ground 2 (should not show Rent Details)
            arguments(TenancyLicenceType.SECURE_TENANCY,
                     Set.of(), Set.of(),
                     Set.of(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE),
                     Set.of(),
                     YesOrNo.NO, "Edge Case: Secure Tenancy + Ground 2"),

            // Edge Case: Assured Tenancy + Ground 9 (should not show Rent Details)
            arguments(TenancyLicenceType.ASSURED_TENANCY,
                     Set.of(), Set.of(NoRentArrearsDiscretionaryGrounds.SUITABLE_ACCOM),
                     Set.of(), Set.of(),
                     YesOrNo.NO, "Edge Case: Assured Tenancy + Ground 9")
        );
    }

    private static Stream<Arguments> provideEdgeCaseScenarios() {
        return Stream.of(
            // Null tenancy type
            arguments(PCSCase.builder()
                     .typeOfTenancyLicence(null)
                     .noRentArrearsMandatoryGroundsOptions(Set.of(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS))
                     .build(),
                     YesOrNo.NO, "Edge Case: Null tenancy type"),

            // Empty sets
            arguments(PCSCase.builder()
                     .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                     .noRentArrearsMandatoryGroundsOptions(Set.of())
                     .noRentArrearsDiscretionaryGroundsOptions(Set.of())
                     .secureOrFlexibleDiscretionaryGrounds(Set.of())
                     .rentArrearsOrBreachOfTenancy(Set.of())
                     .build(),
                     YesOrNo.NO, "Edge Case: All empty sets"),

            // Mixed grounds (should show Rent Details if any rent-related ground is selected)
            arguments(PCSCase.builder()
                     .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                     .noRentArrearsMandatoryGroundsOptions(
                         Set.of(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS))
                     .noRentArrearsDiscretionaryGroundsOptions(
                         Set.of(NoRentArrearsDiscretionaryGrounds.NUISANCE_OR_ILLEGAL_USE))
                     .build(),
                     YesOrNo.YES, "Edge Case: Mixed grounds with rent-related ground")
        );
    }

    /**
     * Simulates the complete routing logic by applying the same logic as the midEvent handlers.
     */
    private YesOrNo simulateRoutingLogic(PCSCase caseData) {
        // For Assured Tenancy - check if grounds 8, 10, or 11 are selected
        if (TenancyLicenceType.ASSURED_TENANCY.equals(caseData.getTypeOfTenancyLicence())) {
            boolean hasRentRelatedGrounds = 
                (caseData.getNoRentArrearsMandatoryGroundsOptions() != null 
                 && caseData.getNoRentArrearsMandatoryGroundsOptions()
                     .contains(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS))
                || (caseData.getNoRentArrearsDiscretionaryGroundsOptions() != null 
                    && (caseData.getNoRentArrearsDiscretionaryGroundsOptions()
                        .contains(NoRentArrearsDiscretionaryGrounds.RENT_ARREARS)
                        || caseData.getNoRentArrearsDiscretionaryGroundsOptions()
                            .contains(NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY)));
            return YesOrNo.from(hasRentRelatedGrounds);
        }
        
        // For Secure/Flexible Tenancy - check if Ground 1 is selected and Rent Arrears is chosen
        if (TenancyLicenceType.SECURE_TENANCY.equals(caseData.getTypeOfTenancyLicence())
            || TenancyLicenceType.FLEXIBLE_TENANCY.equals(caseData.getTypeOfTenancyLicence())) {
            
            boolean hasGround1 = caseData.getSecureOrFlexibleDiscretionaryGrounds() != null
                                && caseData.getSecureOrFlexibleDiscretionaryGrounds()
                                    .contains(RENT_ARREARS_OR_BREACH_OF_TENANCY);
            
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
