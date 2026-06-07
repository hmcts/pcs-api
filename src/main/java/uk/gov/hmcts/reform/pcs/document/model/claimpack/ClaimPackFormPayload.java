package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;
import java.util.List;

/**
 * Typed payload rendered into the claim pack Docmosis template
 * ({@code CV-PCS-CLM-ENG-Claim-Pack.docx}, a single file covering England and Wales).
 *
 * <p>Fields without an entity source yet are still declared here; they render as empty rows
 * until the source is wired, with no template change needed.</p>
 *
 * <p>Mirrors {@link uk.gov.hmcts.reform.pcs.document.model.genapp.GenAppFormPayload} in style
 * ({@code @Data @Builder}, plain fields, no Jackson decorators).</p>
 */
@Data
@Builder
public class ClaimPackFormPayload implements FormPayload {

    // ---------- Title block ----------
    private String referenceNumber;
    private String caseName;
    private LocalDate issueDateSealed;     // No source yet; null until ClaimEntity.issueDate exists.
    private LocalDate submittedOn;
    /**
     * {@code @JsonProperty} keeps the wire key as {@code isWales}: Jackson otherwise strips the
     * {@code is} prefix from boolean getters, but the template tag {@code <<cs_isWales>>} needs the
     * key to stay {@code isWales}, not {@code wales}.
     */
    @JsonProperty("isWales")
    private boolean isWales;
    // Complement of isWales; Docmosis compact syntax can't negate, so we send both.
    @JsonProperty("isEngland")
    private boolean isEngland;

    // ---------- Claimant ----------
    private ClaimPackParty claimant;
    // "Persons unknown", org name, or first + last name. Renders as <<claimantDisplayName>>.
    private String claimantDisplayName;
    // Drive paragraph removal for empty claimant address lines.
    private boolean hasClaimantAddressLine2;
    private boolean hasClaimantAddressLine3;
    private boolean hasClaimantCounty;
    // Wales-only row. "Yes"/"No" from VerticalYesNo.getLabel(); from the Housing (Wales) Act 2014,
    // captured only on the Wales journey, so the row is Wales-gated.
    private boolean showExemptLandlordQuestion;
    private String claimantIsExemptLandlord;

    // ---------- Defendants ----------
    // Flat list rendered by a single <<rs_defendants>>...<<es_>> loop in the template.
    private List<ClaimPackDefendantRow> defendants;

    // ---------- Claim details / grounds ----------
    private ClaimPackAddress propertyAddress;
    // Per-line null suppression for the property address rows.
    private boolean hasPropertyAddressLine2;
    private boolean hasPropertyAddressLine3;
    private boolean hasPropertyCounty;
    @JsonProperty("isIntroDemotedOtherTenancy")
    private boolean isIntroDemotedOtherTenancy;
    // "Yes"/"No" rendered directly by <<hasGroundsYesNo>>.
    private String hasGroundsYesNo;
    // Gate for the "Does the claimant have grounds for possession?" row. Shown only when the
    // tenancy type is introductory, demoted or other; no country qualifier.
    private boolean showGroundsYesNoQuestion;
    // Hide the "Grounds for possession" list row when no grounds are selected, rather than
    // printing a bare label with no value.
    private boolean showGroundsList;
    private List<ClaimPackGround> grounds;
    // Grounds that have a reason; renders the <<rs_groundsWithReasons>>...<<es_>> loop.
    private List<ClaimPackGround> groundsWithReasons;
    private boolean hasOtherGround;
    private String otherGroundsDescription;
    // England and isIntroDemotedOtherTenancy and hasOtherGround.
    private boolean showDescriptionOfGrounds;
    @JsonProperty("isNoOrAbsoluteOrOtherGrounds")
    private boolean isNoOrAbsoluteOrOtherGrounds;
    // No source yet; null until the source field is identified.
    private String whyClaimingPossession;
    // England and isIntroDemotedOtherTenancy and (no grounds or an Other ground).
    private boolean showWhyClaimingPossession;
    // "Yes"/"No".
    private String hasAdditionalReasonsYesNo;
    private boolean additionalReasonsProvided;
    private String additionalReasonsFreeText;

    // ---------- ASB / illegal use / prohibited conduct ----------
    // England-only ASB marker (SECURE_OR_FLEXIBLE_ANTISOCIAL category); not the Wales ASB section gate.
    private boolean hasAsbGround;
    // Wales-only section gate: isWales and any ground has code "ANTISOCIAL_BEHAVIOUR_S157".
    private boolean showAsbSection;
    // "Yes"/"No" rendered directly by the template.
    private String asbAllegedYesNo;
    // Hide the Yes/No row when unanswered (parity with the sibling null-gates).
    private boolean showAsbAlleged;
    private boolean showAsbDetails;
    private String asbDetailsFreeText;
    private String illegalUseAllegedYesNo;
    private boolean showIllegalUseDetails;
    private String illegalUseDetailsFreeText;
    private String otherProhibitedAllegedYesNo;
    private boolean showOtherProhibitedDetails;
    private String otherProhibitedDetailsFreeText;

    // ---------- Rent arrears ----------
    // Whole-section gate for rent arrears.
    private boolean hasRentArrearsGround;
    // GBP strings (e.g. "£1,200.00"); a raw BigDecimal renders as "1200.0", wrong for a money column.
    private String rentAmount;
    private String rentCalculatedDescription;
    private String rentArrearsTotal;
    private String hasPreviousStepsYesNo;
    // Inner gate inside the rent arrears section.
    private boolean showPreviousStepsFreeText;
    private String previousStepsFreeText;
    private String judgmentRequestedYesNo;

    // ---------- Pre-action ----------
    private String preActionProtocolFollowedYesNo;
    // Conditional row gate for the "why not followed" reason.
    private boolean showPreActionProtocolNotFollowedReason;
    private String preActionProtocolNotFollowedReason;
    private String mediationAttemptedYesNo;
    private String settlementAttemptedYesNo;

    // ---------- Notice ----------
    private String noticeServedYesNo;
    // Gates the whole "Method of service onwards" sub-table.
    private boolean noticeServedYes;
    // Derived in the builder; also gates the "why notice not served" template row.
    private boolean noticeNotServedDisplayed;
    private String noticeNotServedReason;
    // Wales-only.
    private String noticeType;
    // Wales-only "Notice type" row gate: isWales and notice served.
    private boolean showNoticeType;
    // Drives the method-of-service gates in the template.
    private NoticeServiceMethod methodOfService;
    private String methodOfServiceLabel;
    // "10 January 2024", derived per serving method in the builder.
    private String noticeServedOn;
    // Show flags for the optional notice rows, set when the value is present.
    private boolean showNoticeServedOn;
    private boolean showNoticeServedTime;
    private boolean showNoticeLeftWithName;
    private boolean showNoticeServedToEmail;
    private boolean showNoticeOtherElectronicDetails;
    private boolean showNoticeOtherMeansDetails;
    // "2:30pm", set only for the date-and-time serving methods.
    private String noticeServedTime;
    // One of four detail fields; only one is set per render, by methodOfService.
    private String noticeLeftWithName;
    private String noticeServedToEmail;
    private String noticeOtherElectronicDetails;
    private String noticeOtherMeansDetails;
    // Whole-row gate for the "can you upload the notice?" rows. Always false for now (no entity
    // captures the answer), so the row is hidden instead of printing a label with a blank value.
    // Set from the answer once a source field is added.
    private boolean showNoticeUploadQuestion;
    private VerticalYesNo noticeUploadedYesNo;
    // Complementary booleans drive the <<cs_noticeUploadedYes>>/<<cs_noticeUploadedNo>> branches.
    private boolean noticeUploadedYes;
    private boolean noticeUploadedNo;
    // No source yet; null until the source field is identified.
    private String noticeNotUploadedReason;

    // ---------- Tenancy / licence ----------
    private String tenancyTypeLabel;
    // "1 April 2022", same format as the served-notice date.
    private String tenancyStartDate;
    // Render the start-date row only when a date is present.
    private boolean showTenancyStartDate;
    private VerticalYesNo tenancyUploadedYesNo;
    // England-only gate: the Wales journey never captures the tenancy-copy answer, so these rows
    // hide on Wales rather than print a blank Yes/No.
    private boolean showTenancyUploadedQuestion;
    // Complementary booleans drive the <<cs_tenancyUploadedYes>>/<<cs_tenancyUploadedNo>> branches.
    private boolean tenancyUploadedYes;
    private boolean tenancyUploadedNo;
    private String tenancyNotUploadedReason;

    // ---------- Circumstances ----------
    private String hasClaimantCircsYesNo;
    private boolean showClaimantCircsFreeText;
    private String claimantCircsFreeText;
    private String hasDefendantCircsYesNo;
    private boolean showDefendantCircsFreeText;
    private String defendantCircsFreeText;

    // ---------- Underlessees / mortgagees ----------
    // Flat list rendered by a single <<rs_underlessees>>...<<es_>> loop, like defendants.
    private String hasUnderlesseeYesNo;
    private List<ClaimPackUnderlesseeRow> underlessees;

    // ---------- Demotion of tenancy ----------
    // Three-layer gating: showIsDemotionClaim wraps the whole section (Y/N row hidden if the user
    // didn't answer); showDemotionDetails wraps the four follow-ups (visible only on Yes);
    // showDemotionTermsFreeText wraps the "Details of terms" row (visible only when the terms
    // were served).
    private boolean showIsDemotionClaim;
    private String isDemotionClaimYesNo;
    private boolean showDemotionDetails;
    private String demotionHousingActSection;
    private String hasServedDemotionTermsYesNo;
    private boolean showDemotionTermsFreeText;
    private String demotionTermsFreeText;
    private String demotionReasonsFreeText;

    // ---------- Suspension of right to buy ----------
    // Two-layer gating: showIsSuspensionClaim wraps the whole section; showSuspensionDetails wraps
    // the two follow-ups (visible only on Yes).
    private boolean showIsSuspensionClaim;
    private String isSuspensionClaimYesNo;
    private boolean showSuspensionDetails;
    private String suspensionHousingActSection;
    private String suspensionReasonsFreeText;

    // ---------- PCSC (Wales) ----------
    // Three-layer gating: showPcscSection (isWales), showPcscDetails (claiming a standard contract),
    // showPcscTermsFreeText (periodic contract agreed).
    private boolean showPcscSection;
    private String isPcscYesNo;
    private boolean showPcscDetails;
    private String pcscTermsAgreedYesNo;
    private boolean showPcscTermsFreeText;
    private String pcscTermsFreeText;
    private String pcscReasonFreeText;

    // ---------- Required documents (Wales) ----------
    // Outer gate: showRequiredDocumentsSection (isWales). Each Y/N has a complementary
    // "showXxxNotUploadedReason" gate, set when the answer is No.
    private boolean showRequiredDocumentsSection;
    private String epcUploadedYesNo;
    private boolean showEpcNotUploadedReason;
    private String epcNotUploadedReason;
    private String gasSafetyUploadedYesNo;
    private boolean showGasSafetyNotUploadedReason;
    private String gasSafetyNotUploadedReason;
    private String eicrUploadedYesNo;
    private boolean showEicrNotUploadedReason;
    private String eicrNotUploadedReason;

    // ---------- Statement of truth ----------
    /**
     * {@code <<cs_signedByClaimant>>} and {@code <<cs_signedByLegalRep>>} each wrap one block;
     * exactly one renders.
     */
    private boolean signedByLegalRep;
    private boolean signedByClaimant;
    /**
     * Signer name and position. The "Full name" and "Position or office held" labels are static
     * text in the template, so these fields carry only the values.
     *
     * <p>The template renders these as a two-column label|value table row, like every other
     * question/answer row, with the gate wrapping the whole row. Without the table the values
     * render bare, with no labels, which is the misalignment this layout avoids.</p>
     */
    private String sotFullName;
    /** Legal-rep variant only. */
    private String sotFirmName;
    private String sotPositionHeld;

}
