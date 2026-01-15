package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ClaimantAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;

/**
 * All possible PCS case states.
 * Converted into CCD states.
 */
@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Awaiting Submission to HMCTS",
        access = {ClaimantAccess.class, CitizenAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    AWAITING_SUBMISSION_TO_HMCTS,

    @CCD(
        label = "Pending Case Issued",
        access = {ClaimantAccess.class, CitizenAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    PENDING_CASE_ISSUED,

    @CCD(
        label = "Case Issued",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, DefendantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    CASE_ISSUED,

    @CCD(
        label = "Case Closed",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    CASE_CLOSED

}

