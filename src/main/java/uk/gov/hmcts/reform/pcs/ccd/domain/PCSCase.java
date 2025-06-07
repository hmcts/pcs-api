package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressGlobalUK;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Applicant's first name")
    private String applicantForename;

    @CCD(label = "")
    private AddressGlobalUK claimPropertyAddress;
}
