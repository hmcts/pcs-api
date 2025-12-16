package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsGroundsOptions;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds.SUITABLE_ACCOM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8;
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

    @ParameterizedTest
    @MethodSource("provideRentArrearsYesFlowScenarios")
    void shouldShowRentDetailsForRentArrearsYesFlow(
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expected) {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsMandatoryGrounds(mandatoryGrounds)
            .rentArrearsDiscretionaryGrounds(discretionaryGrounds)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideNoRentArrearsFlowScenarios")
    void shouldShowRentDetailsForNoRentArrearsFlow(
        Set<NoRentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<NoRentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expected) {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
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
            .typeOfTenancyLicence(ASSURED_TENANCY)
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(null)
                    .discretionaryGrounds(null)
                    .build()
            )
            .claimDueToRentArrears(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenAllGroundsAreNull() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsMandatoryGrounds(null)
            .rentArrearsDiscretionaryGrounds(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowRentDetailsWhenRentArrearsGroundsSetButCanonicalSetsNotPopulated() {
        // This test verifies the fallback logic: when rentArrearsGrounds is set
        // but rentArrearsMandatoryGrounds/rentArrearsDiscretionaryGrounds are null/empty
        // (e.g., when CheckingNotice runs before RentArrearsGroundsForPossession.midEvent())
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .rentArrearsMandatoryGrounds(null)
            .rentArrearsDiscretionaryGrounds(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowRentDetailsWhenRentArrearsGround10SetButCanonicalSetsNotPopulated() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .rentArrearsMandatoryGrounds(Set.of())
            .rentArrearsDiscretionaryGrounds(Set.of())
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowRentDetailsWhenRentArrearsGround11SetButCanonicalSetsNotPopulated() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
            .rentArrearsMandatoryGrounds(Set.of())
            .rentArrearsDiscretionaryGrounds(Set.of())
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldReturnNoWhenRentArrearsGroundsEmptyAndCanonicalSetsNotPopulated() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .rentArrearsGrounds(Set.of())
            .rentArrearsMandatoryGrounds(null)
            .rentArrearsDiscretionaryGrounds(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> provideRentArrearsYesFlowScenarios() {
        return Stream.of(
            // Ground 8 (SERIOUS_RENT_ARREARS_GROUND8) - Should show Rent Details
            arguments(Set.of(SERIOUS_RENT_ARREARS_GROUND8), Set.of(), YesOrNo.YES),

            // Ground 10 (RENT_ARREARS_GROUND10) - Should show Rent Details
            arguments(Set.of(), Set.of(RENT_ARREARS_GROUND10), YesOrNo.YES),

            // Ground 11 (PERSISTENT_DELAY_GROUND11) - Should show Rent Details
            arguments(Set.of(), Set.of(PERSISTENT_DELAY_GROUND11), YesOrNo.YES),

            // All rent arrears grounds - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11),
                YesOrNo.YES
            ),

            // Non-rent grounds only - Should NOT show Rent Details
            arguments(Set.of(), Set.of(), YesOrNo.NO),

            // Mixed grounds with rent-related - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(),
                YesOrNo.YES
            )
        );
    }

    private static Stream<Arguments> provideNoRentArrearsFlowScenarios() {
        return Stream.of(
            // Ground 8 (SERIOUS_RENT_ARREARS) - Should show Rent Details
            arguments(Set.of(SERIOUS_RENT_ARREARS), Set.of(), YesOrNo.YES),

            // Ground 10 (RENT_ARREARS) - Should show Rent Details
            arguments(Set.of(), Set.of(RENT_ARREARS), YesOrNo.YES),

            // Ground 11 (RENT_PAYMENT_DELAY) - Should show Rent Details
            arguments(Set.of(), Set.of(RENT_PAYMENT_DELAY), YesOrNo.YES),

            // All rent arrears grounds - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS),
                Set.of(RENT_ARREARS, RENT_PAYMENT_DELAY),
                YesOrNo.YES
            ),

            // Non-rent grounds only - Should NOT show Rent Details
            arguments(Set.of(ANTISOCIAL_BEHAVIOUR), Set.of(), YesOrNo.NO),
            arguments(Set.of(), Set.of(SUITABLE_ACCOM), YesOrNo.NO),

            // Mixed grounds with rent-related - Should show Rent Details
            arguments(
                Set.of(SERIOUS_RENT_ARREARS),
                Set.of(SUITABLE_ACCOM),
                YesOrNo.YES
            ),

            // No grounds - Should NOT show Rent Details
            arguments(Set.of(), Set.of(), YesOrNo.NO)
        );
    }
}

