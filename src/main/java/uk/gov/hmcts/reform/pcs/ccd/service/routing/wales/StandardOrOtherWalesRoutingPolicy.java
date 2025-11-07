package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingPolicy;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.RENT_ARREARS_SECTION_157;

@Component
public class StandardOrOtherWalesRoutingPolicy implements RentDetailsRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentDetails(PCSCase caseData) {
        Set<DiscretionaryGroundWales> discretionary = caseData.getDiscretionaryGroundsWales();
        if (discretionary == null) {
            return YesOrNo.NO;
        }
        return YesOrNo.from(discretionary.contains(RENT_ARREARS_SECTION_157));
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        // Not selected via tenancy type; Wales service selects by occupation contract type
        return false;
    }
}


