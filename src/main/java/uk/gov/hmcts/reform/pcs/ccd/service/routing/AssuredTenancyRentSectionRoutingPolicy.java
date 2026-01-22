package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds.RENT_ARREARS_GROUND10;
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
            return checkRentArrearsGrounds(caseData);
        } else {
            return checkNoRentArrearsGrounds(caseData);
        }
    }

    private YesOrNo checkRentArrearsGrounds(PCSCase caseData) {
        Set<AssuredMandatoryGrounds> mandatoryGrounds = caseData.getRentArrearsGroundsForPossession()
            .getMandatoryGrounds();
        Set<AssuredDiscretionaryGrounds> discretionaryGrounds = caseData.getRentArrearsGroundsForPossession()
            .getDiscretionaryGrounds();

        // First check the canonical sets (mandatory/discretionary grounds)
        boolean hasRentRelatedGrounds =
            (mandatoryGrounds != null && mandatoryGrounds.contains(SERIOUS_RENT_ARREARS_GROUND8))
            || (discretionaryGrounds != null && (
                discretionaryGrounds.contains(RENT_ARREARS_GROUND10)
                || discretionaryGrounds.contains(PERSISTENT_DELAY_GROUND11)
            ));

        if (hasRentRelatedGrounds) {
            return YesOrNo.YES;
        }

        // Fallback: If canonical sets are null/empty, check rentArrearsGrounds directly
        // This handles cases where rentArrearsGrounds is set but the canonical sets
        // haven't been populated yet (e.g., when CheckingNotice runs before
        // RentArrearsGroundsForPossession.midEvent() updates the sets)
        if ((mandatoryGrounds == null || mandatoryGrounds.isEmpty())
            && (discretionaryGrounds == null || discretionaryGrounds.isEmpty())) {
            Set<RentArrearsGround> rentArrearsGrounds = caseData.getRentArrearsGroundsForPossession()
                .getRentArrearsGrounds();
            if (rentArrearsGrounds != null && !rentArrearsGrounds.isEmpty()) {
                boolean hasRentArrearsGrounds =
                    rentArrearsGrounds.contains(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8)
                    || rentArrearsGrounds.contains(RentArrearsGround.RENT_ARREARS_GROUND10)
                    || rentArrearsGrounds.contains(RentArrearsGround.PERSISTENT_DELAY_GROUND11);
                return YesOrNo.from(hasRentArrearsGrounds);
            }
        }

        return YesOrNo.NO;
    }

    private YesOrNo checkNoRentArrearsGrounds(PCSCase caseData) {
        Set<AssuredMandatoryGrounds> mandatoryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGrounds> discretionaryGrounds =
            caseData.getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();

        if (mandatoryGrounds == null && discretionaryGrounds == null) {
            return YesOrNo.NO;
        }

        boolean hasRentRelatedGrounds =
            (mandatoryGrounds != null && mandatoryGrounds.contains(SERIOUS_RENT_ARREARS_GROUND8))
            || (discretionaryGrounds != null && (
                discretionaryGrounds.contains(AssuredDiscretionaryGrounds.RENT_ARREARS_GROUND10)
                || discretionaryGrounds.contains(AssuredDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11)
            ));

        return YesOrNo.from(hasRentRelatedGrounds);
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        return ASSURED_TENANCY == tenancyType;
    }
}

