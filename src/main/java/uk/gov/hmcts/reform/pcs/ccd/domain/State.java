package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

/**
 * All possible PCS case states.
 * Converted into CCD states.
 */
@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Awaiting Submission to HMCTS",
        access = {CitizenAccess.class}
    )
    AWAITING_SUBMISSION_TO_HMCTS,

    @CCD(
        label = "Case Issued",
        access = {CaseworkerAccess.class}
    )
    CASE_ISSUED,

    @CCD(
        label = "Draft",
        access = {CaseworkerAccess.class}
    )
    DRAFT,

    @CCD(
        label = "Withdrawn"
    )
    WITHDRAWN;

}

