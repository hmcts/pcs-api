package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

class RentDetailsRoutingServiceTest {

    private RentDetailsRoutingService service;
    private AssuredTenancyRoutingPolicy assuredTenancyPolicy;
    private SecureFlexibleRoutingPolicy secureFlexiblePolicy;

    @BeforeEach
    void setUp() {
        assuredTenancyPolicy = new AssuredTenancyRoutingPolicy();
        secureFlexiblePolicy = new SecureFlexibleRoutingPolicy();
        List<RentDetailsRoutingPolicy> policies = Arrays.asList(
            assuredTenancyPolicy,
            secureFlexiblePolicy
        );
        service = new RentDetailsRoutingService(policies);
    }

    @Test
    void shouldDelegateToAssuredTenancyPolicy() {
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .groundsForPossession(YesOrNo.NO)
            .noRentArrearsMandatoryGroundsOptions(null)
            .noRentArrearsDiscretionaryGroundsOptions(null)
            .build();

        YesOrNo result = service.shouldShowRentDetails(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldDelegateToSecureFlexiblePolicy() {
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
            .secureOrFlexiblePossessionGrounds(SecureOrFlexiblePossessionGrounds.builder().build())
            .build();

        YesOrNo result = service.shouldShowRentDetails(caseData);

        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldThrowExceptionForUnsupportedTenancyType() {
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
            .build();

        assertThatThrownBy(() -> service.shouldShowRentDetails(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No routing policy found for tenancy type: INTRODUCTORY_TENANCY");
    }

    @Test
    void shouldHandleAssuredTenancyWithRentArrearsGrounds() {
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .groundsForPossession(YesOrNo.NO)
            .noRentArrearsMandatoryGroundsOptions(
                java.util.Set.of(
                    uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS
                )
            )
            .noRentArrearsDiscretionaryGroundsOptions(null)
            .build();

        YesOrNo result = service.shouldShowRentDetails(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHandleSecureTenancyWithRentArrears() {
        PCSCase caseData = PCSCase.builder()
            .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
            .secureOrFlexiblePossessionGrounds(
                SecureOrFlexiblePossessionGrounds
                    .builder().secureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY)).build())
            .rentArrearsOrBreachOfTenancy(
                java.util.Set.of(
                    uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy.RENT_ARREARS
                )
            )
            .build();

        YesOrNo result = service.shouldShowRentDetails(caseData);

        assertThat(result).isEqualTo(YesOrNo.YES);
    }
}

