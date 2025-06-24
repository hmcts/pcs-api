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
        label = "Open",
        access = {DefaultStateAccess.class}
    )
    Open,

    @CCD(
        label = "Draft",
        access = {DefaultStateAccess.class}
    )
    Draft,

    @CCD(
        label = "Submitted",
        access = {DefaultStateAccess.class}
    )
    Submitted;
}

