package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;

/**
 * Routing policy for Secure and Flexible Tenancy.
 * Shows Rent Details page when Ground 1 (Rent arrears or breach of tenancy) is selected
 * AND user chooses "Rent arrears" option.
 */
@Component
public class SecureFlexibleRentSectionRoutingPolicy implements RentSectionRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds =
            caseData.getSecureOrFlexiblePossessionGrounds().getSecureOrFlexibleDiscretionaryGrounds();

        if (discretionaryGrounds == null || !discretionaryGrounds.contains(RENT_ARREARS_OR_BREACH_OF_TENANCY)) {
            return YesOrNo.NO;
        }

        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreach = caseData.getRentArrearsOrBreachOfTenancy();
        if (rentArrearsOrBreach == null) {
            return YesOrNo.NO;
        }

        boolean hasRentArrears = rentArrearsOrBreach.contains(RENT_ARREARS);
        return YesOrNo.from(hasRentArrears);
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        return SECURE_TENANCY == tenancyType || FLEXIBLE_TENANCY == tenancyType;
    }
}

