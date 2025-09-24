package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * Enum representing rent arrears grounds for possession claims.
 */
public enum RentArrearsGround {

    @CCD(label = "Serious rent arrears (ground 8)")
    SERIOUS_RENT_ARREARS_GROUND8,

    @CCD(label = "Rent arrears (ground 10)")
    RENT_ARREARS_GROUND10,

    @CCD(label = "Persistent delay in paying rent (ground 11)")
    PERSISTENT_DELAY_GROUND11;
}
