package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.RENT_ARREARS_SECTION_157;

@Component
public class StandardOrOtherWalesRentSectionRoutingPolicy implements WalesRentSectionRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        GroundsForPossessionWales grounds = caseData.getGroundsForPossessionWales();
        if (grounds == null) {
            return YesOrNo.NO;
        }
        Set<DiscretionaryGroundWales> discretionary = grounds.getDiscretionaryGroundsWales();
        if (discretionary == null) {
            return YesOrNo.NO;
        }
        return YesOrNo.from(discretionary.contains(RENT_ARREARS_SECTION_157));
    }

    @Override
    public boolean supports(OccupationLicenceTypeWales occupationLicenceType) {
        return occupationLicenceType == OccupationLicenceTypeWales.STANDARD_CONTRACT
            || occupationLicenceType == OccupationLicenceTypeWales.OTHER;
    }
}


