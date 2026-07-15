package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ClaimantAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GlobalSearchAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalCaseFlagAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RasValidationAccess;

/**
 * All possible PCS case states.
 * Converted into CCD states.
 */
@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Awaiting Submission to HMCTS",
        access = {ClaimantAccess.class, CitizenAccess.class, RasValidationAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    AWAITING_SUBMISSION_TO_HMCTS,

    @CCD(
        label = "Pending Case Issued",
        access = {ClaimantAccess.class, CitizenAccess.class,  RasValidationAccess.class,
            InternalCaseFlagAccess.class, GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    PENDING_CASE_ISSUED,

    @CCD(
        label = "Case Issued",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, DefendantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    CASE_ISSUED,

    @CCD(
        label = "Judicial Referral",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    JUDICIAL_REFERRAL,

    @CCD(
        label = "Hearing Readiness",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    HEARING_READINESS,

    @CCD(
        label = "Prepare For Hearing Conduct Hearing",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    PREPARE_FOR_HEARING_CONDUCT_HEARING,

    @CCD(
        label = "Decision Outcome",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    DECISION_OUTCOME,

    @CCD(
        label = "Case Progression",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    CASE_PROGRESSION,

    @CCD(
        label = "All Final Orders",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    ALL_FINAL_ORDERS_ISSUED,

    @CCD(
        label = "Case Stayed",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    CASE_STAYED,

    @CCD(
        label = "Breathing Space",
        access = {CaseworkerReadAccess.class, ClaimantAccess.class, RasValidationAccess.class,
            GlobalSearchAccess.class},
        hint = "${caseTitleMarkdown}"
    )
    BREATHING_SPACE,

    @CCD(
        label = "Closed",
        hint = "${caseTitleMarkdown}"
    )
    CLOSED
}
