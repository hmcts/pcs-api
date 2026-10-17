package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ClaimantAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffNoteReadAccess;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementStatusReadAccess;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.LatestWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

/**
 * The main domain model representing an enforcement order.
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"java:S1133", "java:S6355"})
public class EnforcementOrder {

    // Redundant field to support the existing CCD definition, will be removed in the future once the
    // CCD definition is updated to use the new field
    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @CCD(
        label = "What do you want to apply for?",
        typeOverride = DynamicRadioList
    )
    private DynamicStringList chooseEnforcementType;

    @JsonUnwrapped(prefix = "warrant")
    @CCD
    private WarrantDetails warrantDetails;

    @JsonUnwrapped(prefix = "writ")
    @CCD
    private WritDetails writDetails;

    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated
    @JsonUnwrapped
    @CCD
    private RawWarrantDetails rawWarrantDetails;

    @JsonUnwrapped(prefix = "warrant_rest")
    @CCD
    private WarrantOfRestitutionDetails warrantOfRestitutionDetails;

    @JsonUnwrapped
    @CCD
    private RawWarrantRestDetails rawWarrantRestDetails;

    /**
     * This field is retained only for backwards compatibility.
     *
     * @deprecated As of release 1.4
     */
    @Deprecated
    @CCD(searchable = false)
    @External
    private String warrantFeeAmount;

    @CCD(searchable = false)
    @External
    private String writFeeAmount;

    private String warrantOfRestitutionInfoText;

    @CCD(label = "Are you sure you want to permanently delete this draft application? "
            + "You will no longer be able to view the details in the future.")
    private YesOrNo deleteDraftApplication;

    @CCD(searchable = false)
    private YesOrNo hasUnsubmittedEnforcementData;

    @CCD(searchable = false)
    private String formattedDefendantNames;

    private String formattedPropertyAddress;

    @CCD(searchable = false)
    private String confirmEvictionSummaryMarkup;

    @CCD(searchable = false, access = {ClaimantAccess.class})
    private YesOrNo showConfirmEvictionJourney;

    /**
     * The scheduled eviction date the claimant is being asked to confirm, set for the case view alongside
     * {@link #showConfirmEvictionJourney} and shown on the "The eviction date" page.
     */
    @CCD(searchable = false, access = {ClaimantAccess.class})
    private String formattedEvictionDate;

    /**
     * The claimant's answer in "Confirm the eviction details". It is only used on submit, to move the
     * enforcement status, and is not stored.
     */
    @CCD(label = "Can you attend the eviction on this date?", searchable = false, access = {ClaimantAccess.class})
    private YesOrNo claimantCanAttendEviction;

    /**
     * The status of the case's current enforcement, set for the case view. It is read only: the status is
     * held on the enforcement record and no event edits this field.
     */
    @CCD(label = "Enforcement status", searchable = false, access = {EnforcementStatusReadAccess.class})
    private EnforcementStatus enforcementStatus;

    /**
     * Every warrant and writ on the claim, newest first, set for the case view and shown on the "Enforcement
     * status" tab. Read only, like {@link #enforcementStatus}: the enforcement records hold the data and no
     * event edits this field.
     */
    @CCD(label = "Warrants and writs", searchable = false, access = {EnforcementStatusReadAccess.class})
    private List<ListValue<EnforcementSummary>> enforcements;

    /**
     * "Claimant vs defendants", set for the decideEnforcement header.
     */
    @CCD(searchable = false)
    private String formattedCaseName;

    /**
     * Only set during decideEnforcement. The decision is recorded on the enforcement record.
     */
    @CCD(label = "Do you want to accept or reject this application?", searchable = false)
    private EnforcementDecision enforcementDecision;

    @CCD(label = "Reason for rejecting the application", typeOverride = TextArea, searchable = false)
    private String enforcementRejectionReason;

    /**
     * The note being added in the "Add a bailiff note" event. It is saved to the enforcement on submit,
     * not to the case.
     */
    @CCD(label = "Add note", typeOverride = TextArea, max = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT, searchable = false)
    private String bailiffNoteText;

    /**
     * The current enforcement's saved bailiff notes, newest first, set for the case view. Read only and
     * internal: the notes are held on the enforcement record.
     */
    @CCD(label = "Bailiff note", searchable = false, access = {BailiffNoteReadAccess.class})
    private List<ListValue<BailiffNote>> bailiffNotes;

    @JsonUnwrapped
    @CCD
    private BailiffAllocationDetails bailiffAllocationDetails;

    @JsonUnwrapped
    @CCD
    private RiskLevelDetails riskLevelDetails;

    /**
     * The fields of the schedule, reschedule and notice of eviction events (HDPI-7344, HDPI-7549, HDPI-7346).
     */
    @JsonUnwrapped
    @CCD
    private EvictionSchedule evictionSchedule;

    /**
     * Whether defendants may see the current enforcement, set for the case view. Read only and internal: it is
     * derived on the enforcement record, true once the notice of eviction is served and false when service is
     * dispensed with (HDPI-7658). Nothing reads it yet. Defendant access to the enforcement status tab and the
     * case file view folder is to be driven from it later (HDPI-7539, HDPI-8442).
     */
    @CCD(label = "Defendants can see the enforcement", searchable = false, access = {InternalTabAccess.class})
    private YesOrNo defendantsCanSeeEnforcement;

    /**
     * The answers to the "Record warrant return" event. They are saved to the enforcement's return history on
     * submit, not to the case.
     */
    @JsonUnwrapped(prefix = "warrantReturn")
    @CCD
    private WarrantReturnDetails warrantReturn;

    /**
     * The current enforcement's most recent warrant return, set for the case view. Read only.
     */
    @JsonUnwrapped(prefix = "latestWarrantReturn")
    @CCD
    private LatestWarrantReturn latestWarrantReturn;
}
