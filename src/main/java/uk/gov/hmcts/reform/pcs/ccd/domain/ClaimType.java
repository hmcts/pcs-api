package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;

public enum ClaimType {
    @CCD(label = "Make a possession claim")
    Possession,
    @CCD(label = "Make a possession claim")
    AcceleratedPossession
}
