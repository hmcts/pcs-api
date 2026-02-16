package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157;

@Component
public class SecureWalesRentSectionRoutingPolicy implements WalesRentSectionRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        Set<SecureContractDiscretionaryGroundsWales> discretionary =
            caseData.getSecureContractGroundsForPossessionWales().getDiscretionaryGrounds();
        if (discretionary == null) {
            return YesOrNo.NO;
        }
        return YesOrNo.from(discretionary.contains(RENT_ARREARS_S157));
    }

    @Override
    public boolean supports(OccupationLicenceTypeWales occupationLicenceType) {
        return occupationLicenceType == OccupationLicenceTypeWales.SECURE_CONTRACT;
    }
}


