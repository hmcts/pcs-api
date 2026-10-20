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
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.DefendantVisibleEnforcementAccess;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementStatusReadAccess;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.RiskLevelReadAccess;
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

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"java:S1133", "java:S6355"})
public class EnforcementOrder {

    // Kept because the CCD definition still refers to it; chooseEnforcementType replaces it.
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

    @CCD(searchable = false, access = {ClaimantAccess.class})
    private String formattedEvictionDate;

    @CCD(label = "Can you attend the eviction on this date?", searchable = false, access = {ClaimantAccess.class})
    private YesOrNo claimantCanAttendEviction;

    @CCD(label = "Enforcement status", searchable = false, access = {EnforcementStatusReadAccess.class})
    private EnforcementStatus enforcementStatus;

    /**
     * Whether to show the "Risk assessment" tab. The tab's CCD show condition cannot express the rule, so it is
     * computed here.
     */
    @CCD(label = "Show the risk assessment tab", searchable = false,
        access = {EnforcementStatusReadAccess.class})
    private YesOrNo showRiskAssessmentTab;

    @CCD(label = "Warrants and writs", searchable = false, access = {DefendantVisibleEnforcementAccess.class})
    private List<ListValue<EnforcementSummary>> enforcements;

    @CCD(searchable = false, access = {DefendantVisibleEnforcementAccess.class})
    private String enforcementStatusTabMarkdown;

    @CCD(searchable = false, access = {EnforcementStatusReadAccess.class})
    private String riskAssessmentPrintMarkdown;

    @CCD(searchable = false, access = {RiskLevelReadAccess.class})
    private String riskAssessmentInternalMarkdown;

    @CCD(searchable = false, access = {EnforcementStatusReadAccess.class})
    private String riskAssessmentClaimantMarkdown;

    @CCD(searchable = false)
    private String formattedCaseName;

    @CCD(label = "Do you want to accept or reject this application?", searchable = false)
    private EnforcementDecision enforcementDecision;

    @CCD(label = "Reason for rejecting the application", typeOverride = TextArea, searchable = false)
    private String enforcementRejectionReason;

    @CCD(label = "Add note", typeOverride = TextArea, max = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT, searchable = false)
    private String bailiffNoteText;

    @CCD(label = "Bailiff note", searchable = false, access = {BailiffNoteReadAccess.class})
    private List<ListValue<BailiffNote>> bailiffNotes;

    @JsonUnwrapped
    @CCD
    private BailiffAllocationDetails bailiffAllocationDetails;

    @JsonUnwrapped
    @CCD
    private RiskLevelDetails riskLevelDetails;

    @JsonUnwrapped
    @CCD
    private EvictionSchedule evictionSchedule;

    @CCD(label = "Defendants can see the enforcement", searchable = false, access = {InternalTabAccess.class})
    private YesOrNo defendantsCanSeeEnforcement;

    @JsonUnwrapped(prefix = "warrantReturn")
    @CCD
    private WarrantReturnDetails warrantReturn;

    @JsonUnwrapped(prefix = "latestWarrantReturn")
    @CCD
    private LatestWarrantReturn latestWarrantReturn;

    @JsonUnwrapped
    @CCD
    private EnforcementEventAvailability eventAvailability;
}
