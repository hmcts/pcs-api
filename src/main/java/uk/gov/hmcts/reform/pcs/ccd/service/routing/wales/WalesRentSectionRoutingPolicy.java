package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

/**
 * Wales-specific policy interface for determining whether Rent Details should be shown.
 */
public interface WalesRentSectionRoutingPolicy {

    YesOrNo shouldShowRentSection(PCSCase caseData);

    /**
     * Checks if this policy supports the given occupation licence type.
     *
     * @param occupationLicenceType the occupation licence type to check
     * @return true if this policy can handle the occupation licence type, false otherwise
     */
    boolean supports(OccupationLicenceTypeWales occupationLicenceType);
}


