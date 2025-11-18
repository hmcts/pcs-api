package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

/**
 * Policy interface for determining whether Rent Details page should be shown
 * based on tenancy type and selected grounds.
 */
public interface RentSectionRoutingPolicy {

    /**
     * Determines if Rent Details page should be shown for the given case.
     *
     * @param caseData the case data containing tenancy type and selected grounds
     * @return YesOrNo.YES if rent details should be shown, YesOrNo.NO otherwise
     */
    YesOrNo shouldShowRentSection(PCSCase caseData);

    /**
     * Checks if this policy supports the given tenancy type.
     *
     * @param tenancyType the tenancy type to check
     * @return true if this policy can handle the tenancy type, false otherwise
     */
    boolean supports(TenancyLicenceType tenancyType);
}

