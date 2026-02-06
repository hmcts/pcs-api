package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.RENT_ARREARS_SECTION_157;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181;

@Component
public class StandardOrOtherWalesRentSectionRoutingPolicy implements WalesRentSectionRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        GroundsForPossessionWales grounds = caseData.getGroundsForPossessionWales();
        if (grounds == null) {
            return YesOrNo.NO;
        }
        Set<DiscretionaryGroundWales> discretionary = grounds.getDiscretionaryGroundsWales();
        Set<MandatoryGroundWales> mandatory = grounds.getMandatoryGroundsWales();
        boolean rentArrearsDiscretionary = discretionary != null
                && discretionary.contains(RENT_ARREARS_SECTION_157);
        boolean rentArrearsMandatory = mandatory != null
                && (mandatory.contains(SERIOUS_ARREARS_PERIODIC_S181)
                || mandatory.contains(SERIOUS_ARREARS_FIXED_TERM_S187));
        return YesOrNo.from(rentArrearsDiscretionary || rentArrearsMandatory);
    }

    @Override
    public boolean supports(OccupationLicenceTypeWales occupationLicenceType) {
        return occupationLicenceType == OccupationLicenceTypeWales.STANDARD_CONTRACT
            || occupationLicenceType == OccupationLicenceTypeWales.OTHER;
    }
}


