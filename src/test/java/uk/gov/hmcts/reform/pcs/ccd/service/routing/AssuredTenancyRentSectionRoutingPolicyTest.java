package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround.RENT_ARREARS_GROUND10;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;

class AssuredTenancyRentSectionRoutingPolicyTest {

    private AssuredTenancyRentSectionRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AssuredTenancyRentSectionRoutingPolicy();
    }

    @Test
    void shouldSupportAssuredTenancy() {
        assertThat(policy.supports(ASSURED_TENANCY)).isTrue();
    }

    @Test
    void shouldNotSupportOtherTenancyTypes() {
        assertThat(policy.supports(TenancyLicenceType.SECURE_TENANCY)).isFalse();
        assertThat(policy.supports(TenancyLicenceType.FLEXIBLE_TENANCY)).isFalse();
        assertThat(policy.supports(TenancyLicenceType.INTRODUCTORY_TENANCY)).isFalse();
    }

    @Test
    void shouldAlwaysShowRentDetailsForRentArrearsClaim() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @ParameterizedTest
    @MethodSource("provideNoRentArrearsFlowScenarios")
    void shouldShowRentDetailsForNoRentArrearsFlow(
        Set<AssuredMandatoryGround> mandatoryGrounds,
        Set<AssuredDiscretionaryGround> discretionaryGrounds,
        YesOrNo expected) {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(mandatoryGrounds)
                    .discretionaryGrounds(discretionaryGrounds)
                    .build()
            )
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnNoWhenGroundsForPossessionIsNull() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(null)
                    .discretionaryGrounds(null)
                    .build()
            )
            .claimDueToRentArrears(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> provideNoRentArrearsFlowScenarios() {
        return Stream.of(
            // Ground 8 (SERIOUS_RENT_ARREARS) - Should show Rent Details
            arguments(Set.of(SERIOUS_RENT_ARREARS_GROUND8), Set.of(), YesOrNo.YES),

            // Ground 10 (RENT_ARREARS) - Should show Rent Details
            arguments(Set.of(), Set.of(RENT_ARREARS_GROUND10), YesOrNo.YES),

            // Ground 11 (RENT_PAYMENT_DELAY) - Should show Rent Details
            arguments(Set.of(), Set.of(PERSISTENT_DELAY_GROUND11), YesOrNo.YES),

            // All rent arrears grounds - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11),
                YesOrNo.YES
            ),

            // Non-rent grounds only - Should NOT show Rent Details
            arguments(Set.of(ANTISOCIAL_BEHAVIOUR_GROUND7A), Set.of(), YesOrNo.NO),
            arguments(Set.of(), Set.of(ALTERNATIVE_ACCOMMODATION_GROUND9), YesOrNo.NO),

            // Mixed grounds with rent-related - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(ALTERNATIVE_ACCOMMODATION_GROUND9),
                YesOrNo.YES
            ),

            // No grounds - Should NOT show Rent Details
            arguments(Set.of(), Set.of(), YesOrNo.NO)
        );
    }
}

