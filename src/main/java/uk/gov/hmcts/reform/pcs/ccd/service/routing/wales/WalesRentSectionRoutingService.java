package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.List;

@Service
public class WalesRentSectionRoutingService {

    private final List<WalesRentSectionRoutingPolicy> policies;

    public WalesRentSectionRoutingService(List<WalesRentSectionRoutingPolicy> policies) {
        this.policies = policies;
    }

    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        OccupationLicenceDetailsWales details = caseData.getOccupationLicenceDetailsWales();
        OccupationLicenceTypeWales type = details != null ? details.getOccupationLicenceTypeWales() : null;

        if (type == null) {
            return YesOrNo.NO;
        }

        WalesRentSectionRoutingPolicy matching = null;
        for (WalesRentSectionRoutingPolicy policy : policies) {
            if (policy.supports(type)) {
                if (matching != null) {
                    throw new IllegalStateException(
                        "Multiple Wales routing policies matched: "
                            + matching.getClass().getSimpleName() + ", " + policy.getClass().getSimpleName());
                }
                matching = policy;
            }
        }

        if (matching == null) {
            // No applicable Wales policy for this type
            return YesOrNo.NO;
        }

        return matching.shouldShowRentSection(caseData);
    }
}


