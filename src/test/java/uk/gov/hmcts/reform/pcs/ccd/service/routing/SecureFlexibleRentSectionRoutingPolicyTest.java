package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;

class SecureFlexibleRentSectionRoutingPolicyTest {

    private SecureFlexibleRentSectionRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SecureFlexibleRentSectionRoutingPolicy();
    }

    @Test
    void shouldSupportSecureTenancy() {
        assertThat(policy.supports(SECURE_TENANCY)).isTrue();
    }

    @Test
    void shouldSupportFlexibleTenancy() {
        assertThat(policy.supports(FLEXIBLE_TENANCY)).isTrue();
    }

    @Test
    void shouldNotSupportOtherTenancyTypes() {
        assertThat(policy.supports(TenancyLicenceType.ASSURED_TENANCY)).isFalse();
        assertThat(policy.supports(TenancyLicenceType.INTRODUCTORY_TENANCY)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideSecureFlexibleScenarios")
    void shouldShowRentSectionBasedOnGroundSelection(
        TenancyLicenceType tenancyType,
        Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds,
        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreach,
        YesOrNo expected) {

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(tenancyType)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(discretionaryGrounds).build())
            .rentArrearsOrBreachOfTenancy(rentArrearsOrBreach)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnNoWhenGround1NotSelected() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(Set.of(NUISANCE_OR_IMMORAL_USE)).build())
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenGround1SelectedButBreachOnly() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY)).build())
            .rentArrearsOrBreachOfTenancy(Set.of(BREACH_OF_TENANCY))
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenDiscretionaryGroundsIsNull() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder().build())
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenRentArrearsOrBreachIsNull() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY)).build())
            .rentArrearsOrBreachOfTenancy(null)
            .build();

        YesOrNo result = policy.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> provideSecureFlexibleScenarios() {
        return Stream.of(
            // Secure Tenancy + Ground 1 + Rent Arrears - Should show Rent Details
            arguments(
                SECURE_TENANCY,
                Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                Set.of(RENT_ARREARS),
                YesOrNo.YES
            ),

            // Flexible Tenancy + Ground 1 + Rent Arrears - Should show Rent Details
            arguments(
                FLEXIBLE_TENANCY,
                Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                Set.of(RENT_ARREARS),
                YesOrNo.YES
            ),

            // Secure Tenancy + Ground 1 + Breach Only - Should NOT show Rent Details
            arguments(
                SECURE_TENANCY,
                Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                Set.of(BREACH_OF_TENANCY),
                YesOrNo.NO
            ),

            // Flexible Tenancy + Ground 1 + Breach Only - Should NOT show Rent Details
            arguments(
                FLEXIBLE_TENANCY,
                Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                Set.of(BREACH_OF_TENANCY),
                YesOrNo.NO
            ),

            // Secure Tenancy + Ground 1 + Both Rent and Breach - Should show Rent Details
            arguments(
                SECURE_TENANCY,
                Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                Set.of(RENT_ARREARS, BREACH_OF_TENANCY),
                YesOrNo.YES
            ),

            // Secure Tenancy + Other Grounds Only - Should NOT show Rent Details
            arguments(
                SECURE_TENANCY,
                Set.of(NUISANCE_OR_IMMORAL_USE),
                Set.of(),
                YesOrNo.NO
            )
        );
    }
}

