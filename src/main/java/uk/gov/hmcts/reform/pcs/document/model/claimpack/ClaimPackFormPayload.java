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
 * ({@code CV-PCS-CLM-ENG-Claim-Pack.docx} — single file covers both England and Wales).
 *
 * <p>Field-by-field source mapping is in plan §13.2; visibility rules driven by {@code R-*} gates
 * are catalogued in §6.5. Fields without a current entity source (§13.3 gaps) are still declared
 * here — they'll render as empty rows until the gap is closed, no template change needed.</p>
 *
 * <p>Mirrors {@link uk.gov.hmcts.reform.pcs.document.model.genapp.GenAppFormPayload} in style
 * ({@code @Data @Builder}, plain fields, no Jackson decorators).</p>
 */
@Data
@Builder
public class ClaimPackFormPayload implements FormPayload {

    // ---------- §6.3.1 Title block ----------
    private String referenceNumber;
    private String caseName;
    private LocalDate issueDateSealed;     // §13.3 gap (1) — null until ClaimEntity.issueDate exists
    private LocalDate submittedOn;
    /**
     * Drives every {@code R-COUNTRY-*} rule in §6.5.
     *
     * <p>{@code @JsonProperty} overrides Jackson's Bean-Property convention that strips
     * the {@code is} prefix from boolean getters — the template tag {@code <<cs_isWales>>}
     * requires the wire JSON key to remain {@code isWales}, not {@code wales}.</p>
     */
    @JsonProperty("isWales")
    private boolean isWales;
    // Complement of isWales — Docmosis compact syntax can't negate, so we send both.
    @JsonProperty("isEngland")
    private boolean isEngland;

    // ---------- §6.3.2 Claimant ----------
    private ClaimPackParty claimant;
    // Pre-rendered "Persons unknown" / orgName / firstName+lastName — drives <<claimantDisplayName>>.
    private String claimantDisplayName;
    // Per-line null suppression — drive <<cs_hasClaimantAddressLine*>> paragraph removal.
    private boolean hasClaimantAddressLine2;
    private boolean hasClaimantAddressLine3;
    private boolean hasClaimantCounty;
    // Wales-only row in §6.3.2. Title-case "Yes"/"No" pre-converted from VerticalYesNo.getLabel().
    // Housing (Wales) Act 2014 — captured only on the Wales journey, so the row is Wales-gated.
    private boolean showExemptLandlordQuestion;
    private String claimantIsExemptLandlord;

    // ---------- §6.3.3 – §6.3.6 Defendants ----------
    // Flat list rendered by a single <<rs_defendants>>...<<es_>> loop in the template.
    // Replaces the old defendant1 + additionalDefendants split — see plan §13.2.
    private List<ClaimPackDefendantRow> defendants;

    // ---------- §6.3.7 Claim details / grounds ----------
    private ClaimPackAddress propertyAddress;
    // Per-line null suppression for the property address rows.
    private boolean hasPropertyAddressLine2;
    private boolean hasPropertyAddressLine3;
    private boolean hasPropertyCounty;
    /** Drives R-TENANCY-INTRO-DEMOTED-OTHER. */
    @JsonProperty("isIntroDemotedOtherTenancy")
    private boolean isIntroDemotedOtherTenancy;
    // Title-case "Yes"/"No" — rendered directly by <<hasGroundsYesNo>>.
    private String hasGroundsYesNo;
    // Gate for the "Does the claimant have grounds for possession?" row.
    // Excel mapping row D9 + Cook comment [12]: "Mandatory to be displayed only if the
    // tenancy type is intro, demoted or other" — no country qualifier (compare D11/D13
    // which explicitly say "English journey"). So this is purely a tenancy-type check.
    private boolean showGroundsYesNoQuestion;
    // Gate for the "Grounds for possession" list row (Excel D10): hide the row entirely
    // when no grounds are selected (e.g. claimant answered "No" to D9) rather than printing
    // a bare label with no value.
    private boolean showGroundsList;
    private List<ClaimPackGround> grounds;
    // Filtered subset of grounds with reasonFreeText — drives the
    // <<rs_groundsWithReasons>>...<<es_>> loop in the template.
    private List<ClaimPackGround> groundsWithReasons;
    /** Drives R-GROUND-OTHER. */
    private boolean hasOtherGround;
    private String otherGroundsDescription;
    // England + isIntroDemotedOtherTenancy + hasOtherGround.
    private boolean showDescriptionOfGrounds;
    /** Drives R-NO-OR-ABSOLUTE-OR-OTHER-GROUNDS. */
    @JsonProperty("isNoOrAbsoluteOrOtherGrounds")
    private boolean isNoOrAbsoluteOrOtherGrounds;
    /** §13.3 gap (2) — null until source field is identified. */
    private String whyClaimingPossession;
    // England + isIntroDemotedOtherTenancy + (hasGroundsYesNo == NO || hasOtherGround).
    private boolean showWhyClaimingPossession;
    // Title-case "Yes"/"No".
    private String hasAdditionalReasonsYesNo;
    /** Drives R-ADDITIONAL-REASONS-PROVIDED. */
    private boolean additionalReasonsProvided;
    private String additionalReasonsFreeText;

    // ---------- §6.3.8 ASB / illegal use / prohibited conduct ----------
    /** England-only ASB marker (SECURE_OR_FLEXIBLE_ANTISOCIAL category) — not used by the
     *  Wales-only ASB section gate. */
    private boolean hasAsbGround;
    // Section gate (Wales-only): isWales AND any ground has code "ANTISOCIAL_BEHAVIOUR_S157".
    private boolean showAsbSection;
    // Title-case "Yes"/"No" rendered directly by the template.
    private String asbAllegedYesNo;
    // Gate for the D16 Yes/No row — hide when unanswered (parity with the D18/D20 null-gates).
    private boolean showAsbAlleged;
    private boolean showAsbDetails;
    private String asbDetailsFreeText;
    private String illegalUseAllegedYesNo;
    private boolean showIllegalUseDetails;
    private String illegalUseDetailsFreeText;
    private String otherProhibitedAllegedYesNo;
    private boolean showOtherProhibitedDetails;
    private String otherProhibitedDetailsFreeText;

    // ---------- §6.3.9 Rent arrears ----------
    /** Drives R-RENT-ARREARS-GROUND (whole-section gate). */
    private boolean hasRentArrearsGround;
    // Pre-formatted GBP currency strings (e.g. "£1,200.00") — Docmosis renders
    // raw BigDecimal as "1200.0" which is wrong for a money column.
    private String rentAmount;
    private String rentCalculatedDescription;
    private String rentArrearsTotal;
    private String hasPreviousStepsYesNo;
    // Inner gate inside the rent arrears section — drives <<cs_showPreviousStepsFreeText>>.
    private boolean showPreviousStepsFreeText;
    private String previousStepsFreeText;
    private String judgmentRequestedYesNo;

    // ---------- §6.3.10 Pre-action ----------
    private String preActionProtocolFollowedYesNo;
    // Conditional row gate — drives <<cs_showPreActionProtocolNotFollowedReason>>.
    private boolean showPreActionProtocolNotFollowedReason;
    private String preActionProtocolNotFollowedReason;
    private String mediationAttemptedYesNo;
    private String settlementAttemptedYesNo;

    // ---------- §6.3.11 Notice ----------
    private String noticeServedYesNo;
    // Positive boolean — gates the whole "Method of service onwards" sub-table.
    private boolean noticeServedYes;
    // Derived in builder; also gates the "why notice not served" template row.
    private boolean noticeNotServedDisplayed;
    private String noticeNotServedReason;
    /** Wales-only. */
    private String noticeType;
    // Wales-only "Notice type" row gate — isWales AND noticeServedYesNo == YES.
    private boolean showNoticeType;
    /** Used to drive R-METHOD-* gates inside the template. */
    private NoticeServiceMethod methodOfService;
    private String methodOfServiceLabel;
    // Pre-formatted "10 January 2024" — derived per serving method in the builder (date-only
    // methods use noticeDate; date+time methods derive the date from noticeDateTime).
    private String noticeServedOn;
    // Per-value-presence show flags for optional notice rows (Excel mapping 37–42).
    private boolean showNoticeServedOn;
    private boolean showNoticeServedTime;
    private boolean showNoticeLeftWithName;
    private boolean showNoticeServedToEmail;
    private boolean showNoticeOtherElectronicDetails;
    private boolean showNoticeOtherMeansDetails;
    // Pre-formatted "2:30pm" — only populated for the date+time serving methods.
    private String noticeServedTime;
    /** Four shared-storage detail fields — only one is populated per render, driven by methodOfService. */
    private String noticeLeftWithName;
    private String noticeServedToEmail;
    private String noticeOtherElectronicDetails;
    private String noticeOtherMeansDetails;
    // Whole-row gate (<<cr_showNoticeUploadQuestion>>) for the "can you upload the notice?" rows
    // (R43/R44). Currently always false — no entity captures the answer — so the row is HIDDEN
    // rather than printing an orphaned label with a blank value. Set to (answer present) once a
    // source field is added.
    private boolean showNoticeUploadQuestion;
    private VerticalYesNo noticeUploadedYesNo;
    // Complementary booleans drive <<cs_noticeUploadedYes>>/<<cs_noticeUploadedNo>> branches.
    private boolean noticeUploadedYes;
    private boolean noticeUploadedNo;
    /** §13.3 gap (3) — null until source field is identified. */
    private String noticeNotUploadedReason;

    // ---------- §6.3.12 Tenancy / licence ----------
    private String tenancyTypeLabel;
    // Pre-formatted "1 April 2022" (consistent with the served-notice date).
    private String tenancyStartDate;
    // Show flag — render the start-date row only if a date was provided.
    private boolean showTenancyStartDate;
    private VerticalYesNo tenancyUploadedYesNo;
    // England-only gate for D49/D50 (Cook [35]/[36]): the Wales journey never captures the
    // tenancy-copy answer, so these rows must hide on Wales rather than print a blank Yes/No.
    private boolean showTenancyUploadedQuestion;
    // Complementary booleans drive <<cs_tenancyUploadedYes>>/<<cs_tenancyUploadedNo>> branches.
    private boolean tenancyUploadedYes;
    private boolean tenancyUploadedNo;
    private String tenancyNotUploadedReason;

    // ---------- §6.3.13 / §6.3.14 Circumstances ----------
    private String hasClaimantCircsYesNo;
    private boolean showClaimantCircsFreeText;
    private String claimantCircsFreeText;
    private String hasDefendantCircsYesNo;
    private boolean showDefendantCircsFreeText;
    private String defendantCircsFreeText;

    // ---------- §6.3.15 Underlessees / mortgagees ----------
    // Flat list rendered by a single <<rs_underlessees>>...<<es_>> loop in the template
    // (mirrors defendants pattern). Address-unknown semantic per Excel mapping row 58/60.
    private String hasUnderlesseeYesNo;
    private List<ClaimPackUnderlesseeRow> underlessees;

    // ---------- §6.3.16 Demotion of tenancy ----------
    // Three-layer gating: showIsDemotionClaim wraps whole section (Y/N row hidden if user
    // didn't answer); showDemotionDetails wraps the 4 follow-ups (visible only when Y);
    // showDemotionTermsFreeText wraps the "Details of terms" row (visible only when
    // hasServedDemotionTermsYesNo == YES).
    private boolean showIsDemotionClaim;
    private String isDemotionClaimYesNo;
    private boolean showDemotionDetails;
    private String demotionHousingActSection;
    private String hasServedDemotionTermsYesNo;
    private boolean showDemotionTermsFreeText;
    private String demotionTermsFreeText;
    private String demotionReasonsFreeText;

    // ---------- §6.3.17 Suspension of right to buy ----------
    // Two-layer gating: showIsSuspensionClaim wraps whole section; showSuspensionDetails
    // wraps the 2 follow-ups (visible only when Y).
    private boolean showIsSuspensionClaim;
    private String isSuspensionClaimYesNo;
    private boolean showSuspensionDetails;
    private String suspensionHousingActSection;
    private String suspensionReasonsFreeText;

    // ---------- §6.3.18 PCSC (Wales) ----------
    // Three-layer gating: showPcscSection (isWales), showPcscDetails (isPcscYesNo == YES),
    // showPcscTermsFreeText (pcscTermsAgreedYesNo == YES).
    private boolean showPcscSection;
    private String isPcscYesNo;
    private boolean showPcscDetails;
    private String pcscTermsAgreedYesNo;
    private boolean showPcscTermsFreeText;
    private String pcscTermsFreeText;
    private String pcscReasonFreeText;

    // ---------- §6.3.19 Required documents (Wales) — §13.3 gap (4) ----------
    // Outer gate: showRequiredDocumentsSection (isWales). Each Y/N has a complementary
    // "showXxxNotUploadedReason" gate (xxxUploadedYesNo == NO).
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

    // ---------- §6.3.20 / §6.3.21 Statement of truth ----------
    /** Template gates: {@code <<cs_signedByClaimant>>} wraps the §6.3.20 block;
     *  {@code <<cs_signedByLegalRep>>} wraps the §6.3.21 block. Exactly one renders. */
    private boolean signedByLegalRep;
    private boolean signedByClaimant;
    /**
     * Signer name and position. The labels "Full name" / "Position or office held" are STATIC
     * text in the Docmosis template, not payload data — these fields carry only the values.
     *
     * <p><b>Template note:</b> the template MUST render these as a fixed two-column label|value
     * table row (label cell | {@code <<sotFullName>>} cell), matching every other question/answer
     * row, with the conditional gate wrapping the whole row. Do NOT revert the signer rows to
     * tabs, manual spaces, or free text: without the table the values render bare ("John" /
     * "Solicitor" with no labels, left of the value column), which is the misalignment bug this
     * layout fixes.</p>
     */
    private String sotFullName;
    /** Legal-rep variant only. */
    private String sotFirmName;
    private String sotPositionHeld;

}
