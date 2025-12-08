package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service for determining whether Rent Details page should be shown
 * based on tenancy type and selected grounds.
 * Delegates to appropriate policy implementation based on tenancy type.
 */
@Service
public class RentSectionRoutingService {

    private final Map<TenancyLicenceType, RentSectionRoutingPolicy> policyMap;

    public RentSectionRoutingService(List<RentSectionRoutingPolicy> policies) {
        this.policyMap = buildPolicyMap(policies);
    }

    /**
     * Computes whether Rent Details page should be shown for the given case.
     *
     * @param caseData the case data containing tenancy type and selected grounds
     * @return YesOrNo.YES if rent details should be shown, YesOrNo.NO otherwise
     * @throws IllegalStateException if no routing policy is found for the tenancy type
     */
    public YesOrNo shouldShowRentSection(PCSCase caseData) {
        TenancyLicenceDetails tenancyDetails =
            caseData.getTenancyLicenceDetails();
        TenancyLicenceType tenancyType = tenancyDetails != null
            ? tenancyDetails.getTypeOfTenancyLicence() : null;
        RentSectionRoutingPolicy policy = getPolicyOrThrow(tenancyType);
        return policy.shouldShowRentSection(caseData);
    }

    private RentSectionRoutingPolicy getPolicyOrThrow(TenancyLicenceType tenancyType) {
        RentSectionRoutingPolicy policy = policyMap.get(tenancyType);
        if (policy == null) {
            throw new IllegalStateException(
                "No routing policy found for tenancy type: " + tenancyType
                    + ". A policy implementation must be provided for all supported tenancy types.");
        }
        return policy;
    }

    private Map<TenancyLicenceType, RentSectionRoutingPolicy> buildPolicyMap(
        List<RentSectionRoutingPolicy> policies) {
        Map<TenancyLicenceType, RentSectionRoutingPolicy> map =
            new EnumMap<>(TenancyLicenceType.class);

        for (TenancyLicenceType tenancyType : TenancyLicenceType.values()) {
            for (RentSectionRoutingPolicy policy : policies) {
                if (policy.supports(tenancyType)) {
                    RentSectionRoutingPolicy existing = map.putIfAbsent(tenancyType, policy);
                    if (existing != null) {
                        throw new IllegalStateException(
                            "Multiple routing policies found for tenancy type: " + tenancyType
                                + ". Existing: " + existing.getClass().getSimpleName()
                                + ", Replacement: " + policy.getClass().getSimpleName());
                    }
                }
            }
        }

        return map;
    }
}

