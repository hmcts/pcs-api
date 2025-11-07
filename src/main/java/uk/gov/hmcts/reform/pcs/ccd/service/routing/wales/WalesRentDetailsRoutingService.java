package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingPolicy;

import java.util.List;

@Service
public class WalesRentDetailsRoutingService {

    private final List<RentDetailsRoutingPolicy> policies;

    public WalesRentDetailsRoutingService(List<RentDetailsRoutingPolicy> policies) {
        this.policies = policies;
    }

    public YesOrNo shouldShowRentDetails(PCSCase caseData) {
        OccupationLicenceDetailsWales details = caseData.getOccupationLicenceDetailsWales();
        OccupationLicenceTypeWales type = details != null ? details.getOccupationLicenceTypeWales() : null;

        RentDetailsRoutingPolicy matching;
        if (type == OccupationLicenceTypeWales.SECURE_CONTRACT) {
            matching = policies.stream()
                .filter(p -> p instanceof SecureWalesRoutingPolicy)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SecureWalesRoutingPolicy bean not found"));
        } else if (type == OccupationLicenceTypeWales.STANDARD_CONTRACT || type == OccupationLicenceTypeWales.OTHER) {
            matching = policies.stream()
                .filter(p -> p instanceof StandardOrOtherWalesRoutingPolicy)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("StandardOrOtherWalesRoutingPolicy bean not found"));
        } else {
            // No applicable Wales routing
            return YesOrNo.NO;
        }

        return matching.shouldShowRentDetails(caseData);
    }
}


