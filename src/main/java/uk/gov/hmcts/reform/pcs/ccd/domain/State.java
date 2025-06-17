package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * All possible PCS case states.
 * Converted into CCD states.
 */
@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Draft",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    Draft,

    @CCD(
        label = "Submitted",
        access = {CaseworkerAccess.class}
    )
    Submitted
}

