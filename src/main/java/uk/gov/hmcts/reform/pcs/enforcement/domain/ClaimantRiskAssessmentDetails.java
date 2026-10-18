package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementStatusReadAccess;

import java.util.List;

/**
 * The claimant's risk assessment for the current warrant, original and updated (HDPI-7552), set for the case
 * view. Read only: no event edits these fields. Defendants cannot see them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantRiskAssessmentDetails {

    @CCD(label = "Claimant risk assessment", searchable = false, access = {EnforcementStatusReadAccess.class})
    private List<ListValue<ClaimantRiskAnswer>> claimantRiskAnswers;

    /**
     * When the claimant gave their original answers, in their application.
     */
    @CCD(label = "Original answers given on", searchable = false, access = {EnforcementStatusReadAccess.class})
    private String claimantRiskAnswersGivenOn;

    /**
     * When the claimant last changed their answers. Empty if they never have.
     */
    @CCD(label = "Answers updated on", searchable = false, access = {EnforcementStatusReadAccess.class})
    private String claimantRiskAnswersUpdatedOn;

    /**
     * Whether the claimant has changed their answers since the bailiff last set the risk level, so the bailiff
     * needs to review them.
     */
    @CCD(label = "Updated risk information to review", searchable = false,
        access = {EnforcementStatusReadAccess.class})
    private YesOrNo claimantRiskUpdateToReview;
}
