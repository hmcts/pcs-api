package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1;

class RentSectionRoutingServiceTest {

    private RentSectionRoutingService service;

    @BeforeEach
    void setUp() {
        AssuredTenancyRentSectionRoutingPolicy assuredTenancyPolicy = new AssuredTenancyRentSectionRoutingPolicy();
        SecureFlexibleRentSectionRoutingPolicy secureFlexiblePolicy = new SecureFlexibleRentSectionRoutingPolicy();

        List<RentSectionRoutingPolicy> policies = Arrays.asList(
                assuredTenancyPolicy,
                secureFlexiblePolicy
        );
        service = new RentSectionRoutingService(policies);
    }

    @Test
    void shouldDelegateToAssuredTenancyPolicy() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(null)
                    .discretionaryGrounds(null)
                    .build()
            )
            .build();

        YesOrNo result = service.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldDelegateToSecureFlexiblePolicy() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder().build())
            .build();

        YesOrNo result = service.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldThrowExceptionForUnsupportedTenancyType() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                    .build()
            )
            .build();

        assertThatThrownBy(() -> service.shouldShowRentSection(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No routing policy found for tenancy type: INTRODUCTORY_TENANCY");
    }

    @Test
    void shouldHandleAssuredTenancyWithRentArrearsGrounds() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.NO)
            .noRentArrearsGroundsOptions(
                AssuredNoArrearsPossessionGrounds.builder()
                    .mandatoryGrounds(Set.of(SERIOUS_RENT_ARREARS_GROUND8))
                    .discretionaryGrounds(null)
                    .build()
            )
            .build();

        YesOrNo result = service.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHandleSecureTenancyWithRentArrears() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
                    .build()
            )
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder()
                    .secureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1))
                    .build()
            )
            .rentArrearsOrBreachOfTenancy(
                Set.of(
                    RentArrearsOrBreachOfTenancy.RENT_ARREARS
                )
            )
            .build();

        YesOrNo result = service.shouldShowRentSection(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }
}

