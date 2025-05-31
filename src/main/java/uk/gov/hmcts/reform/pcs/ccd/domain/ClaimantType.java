package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;

public enum ClaimantType {
    @CCD(label = "Local Authority")
    LocalAuthority,
    Mortgagee,
    Other,
}
