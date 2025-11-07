package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingPolicy;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.RENT_ARREARS;

@Component
public class SecureWalesRoutingPolicy implements RentDetailsRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentDetails(PCSCase caseData) {
        Set<SecureContractDiscretionaryGroundsWales> discretionary =
            caseData.getSecureContractDiscretionaryGroundsWales();
        if (discretionary == null) {
            return YesOrNo.NO;
        }
        return YesOrNo.from(discretionary.contains(RENT_ARREARS));
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        // Not selected via tenancy type; Wales service selects by occupation contract type
        return false;
    }
}


