package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;
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
        access = {ClaimantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    AWAITING_SUBMISSION_TO_HMCTS,

    @CCD(
        label = "Awaiting Validation",
        access = {ClaimantAccess.class, CaseworkerReadAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    AWAITING_CLAIM_VALIDATION,

    @CCD(
        label = "Pending Case Issued",
        access = {ClaimantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    PENDING_CASE_ISSUED,

    @CCD(
        label = "Claim struck out",
        access = {ClaimantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    STRUCK_OUT,

    @CCD(
        label = "Case Issued",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, DefendantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    CASE_ISSUED,

    @CCD(
        label = "Requested for deletion",
        access = {ClaimantAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    REQUESTED_FOR_DELETION
}

