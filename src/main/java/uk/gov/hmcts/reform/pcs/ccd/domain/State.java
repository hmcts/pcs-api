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
        access = {DraftStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    Draft,

    @CCD(
        label = "Open",
        access = {DraftStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    Open,

    @CCD(
        label = "Pending Case Issued",
        access = {DraftStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    PendingCaseIssued,

    @CCD(
        label = "Case Issued",
        access = {DefaultStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    CaseIssued,

    @CCD(
        label = "Breathing Space",
        access = {DefaultStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    BreathingSpace,

    @CCD(
        label = "Judicial Referral",
        access = {DefaultStateAccess.class},
        hint = "${pageHeadingMarkdown}"
    )
    JudicialReferral;

}

