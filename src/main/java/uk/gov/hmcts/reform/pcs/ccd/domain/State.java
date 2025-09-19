package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ClaimantAccess;

/**
 * All possible PCS case states.
 * Converted into CCD states.
 */
@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Awaiting further claim details",
        access = {ClaimantAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    AWAITING_FURTHER_CLAIM_DETAILS,

    @CCD(
        label = "Awaiting Submission to HMCTS",
        access = {ClaimantAccess.class, CitizenAccess.class}
    )
    AWAITING_SUBMISSION_TO_HMCTS,

    @CCD(
        label = "Case Issued",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    CASE_ISSUED

}

