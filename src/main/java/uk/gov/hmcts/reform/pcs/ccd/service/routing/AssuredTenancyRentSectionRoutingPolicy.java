package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;

/**
 * Routing policy for Assured Tenancy.
 * Handles both "Yes" and "No" flows for grounds for possession.
 * Shows Rent Details page when grounds 8, 10, or 11 are selected.
 */
@Component
public class AssuredTenancyRentSectionRoutingPolicy implements RentSectionRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        if (caseData.getClaimDueToRentArrears() == YesOrNo.YES) {
            return YesOrNo.YES;
        } else {
            return checkNoRentArrearsGrounds(caseData);
        }
    }

    private YesOrNo checkNoRentArrearsGrounds(PCSCase caseData) {
        Set<AssuredMandatoryGround> mandatoryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> discretionaryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();

        if (mandatoryGrounds == null && discretionaryGrounds == null) {
            return YesOrNo.NO;
        }

        boolean hasRentRelatedGrounds =
            (mandatoryGrounds != null && mandatoryGrounds.contains(SERIOUS_RENT_ARREARS_GROUND8))
            || (discretionaryGrounds != null && (
                discretionaryGrounds.contains(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10)
                || discretionaryGrounds.contains(AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11)
            ));

        return YesOrNo.from(hasRentRelatedGrounds);
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        return ASSURED_TENANCY == tenancyType;
    }
}

