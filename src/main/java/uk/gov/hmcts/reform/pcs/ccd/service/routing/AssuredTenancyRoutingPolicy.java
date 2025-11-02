package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds.RENT_PAYMENT_DELAY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;

/**
 * Routing policy for Assured Tenancy.
 * Handles both "Yes" and "No" flows for grounds for possession.
 * Shows Rent Details page when grounds 8, 10, or 11 are selected.
 */
@Component
@Slf4j
public class AssuredTenancyRoutingPolicy implements RentDetailsRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentDetails(PCSCase caseData) {
        if (caseData.getGroundsForPossession() == YesOrNo.YES) {
            return checkRentArrearsGrounds(caseData);
        } else {
            return checkNoRentArrearsGrounds(caseData);
        }
    }

    private YesOrNo checkRentArrearsGrounds(PCSCase caseData) {
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds = caseData.getRentArrearsMandatoryGrounds();
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds = caseData.getRentArrearsDiscretionaryGrounds();

        if (mandatoryGrounds == null && discretionaryGrounds == null) {
            return YesOrNo.NO;
        }

        boolean hasRentRelatedGrounds =
            (mandatoryGrounds != null && mandatoryGrounds.contains(SERIOUS_RENT_ARREARS_GROUND8))
            || (discretionaryGrounds != null && (
                discretionaryGrounds.contains(RENT_ARREARS_GROUND10)
                || discretionaryGrounds.contains(PERSISTENT_DELAY_GROUND11)
            ));

        return YesOrNo.from(hasRentRelatedGrounds);
    }

    private YesOrNo checkNoRentArrearsGrounds(PCSCase caseData) {
        Set<NoRentArrearsMandatoryGrounds> mandatoryGrounds = caseData.getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> discretionaryGrounds =
            caseData.getNoRentArrearsDiscretionaryGroundsOptions();

        if (mandatoryGrounds == null && discretionaryGrounds == null) {
            return YesOrNo.NO;
        }

        boolean hasRentRelatedGrounds =
            (mandatoryGrounds != null && mandatoryGrounds.contains(SERIOUS_RENT_ARREARS))
            || (discretionaryGrounds != null && (
                discretionaryGrounds.contains(RENT_ARREARS)
                || discretionaryGrounds.contains(RENT_PAYMENT_DELAY)
            ));

        return YesOrNo.from(hasRentRelatedGrounds);
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        return ASSURED_TENANCY == tenancyType;
    }
}

