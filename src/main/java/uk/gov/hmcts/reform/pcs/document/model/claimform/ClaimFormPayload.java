package uk.gov.hmcts.reform.pcs.document.model.claimform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;
import java.util.List;

/**
 * Typed payload rendered into the claim form Docmosis template
 * ({@code CV-PCS-CLM-ENG-Claim-Pack.docx}, a single file covering England and Wales).
 *
 * <p>Values are pre-formatted by {@link ClaimFormPayloadBuilder}; the {@code show*} flags gate the
 * matching template rows/sections. Fields without an entity source yet are declared here and render
 * as empty rows until the source is wired, with no template change needed.</p>
 */
@Data
@Builder
public class ClaimFormPayload implements FormPayload {

    // ---------- Title block ----------
    private String referenceNumber;
    private String caseName;
    private LocalDate issueDateSealed;     // from ClaimEntity.claimIssuedDate (set on payment/issue)
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
    private ClaimFormParty claimant;
    private String claimantDisplayName;
    private boolean hasClaimantAddressLine2;
    private boolean hasClaimantAddressLine3;
    private boolean hasClaimantCounty;
    private boolean showExemptLandlordQuestion;
    private String claimantIsExemptLandlord;

    // ---------- Defendants ----------
    private List<ClaimFormDefendantRow> defendants;

    // ---------- Claim details / grounds ----------
    private ClaimFormAddress propertyAddress;
    private boolean hasPropertyAddressLine2;
    private boolean hasPropertyAddressLine3;
    private boolean hasPropertyCounty;
    @JsonProperty("isIntroDemotedOtherTenancy")
    private boolean isIntroDemotedOtherTenancy;
    private String hasGroundsYesNo;
    private boolean showGroundsYesNoQuestion;
    private boolean showGroundsList;
    private List<ClaimFormGround> grounds;
    private List<ClaimFormGround> groundsWithReasons;
    private boolean hasOtherGround;
    private String otherGroundsDescription;
    private boolean showDescriptionOfGrounds;
    @JsonProperty("isNoOrAbsoluteOrOtherGrounds")
    private boolean isNoOrAbsoluteOrOtherGrounds;
    // One row per Absolute/Other/No-grounds ground; nameAndNumber is the bracket label (null = no-grounds).
    private List<ClaimFormGround> whyClaimingPossessionGrounds;
    private String hasAdditionalReasonsYesNo;
    private boolean additionalReasonsProvided;
    private String additionalReasonsFreeText;

    // ---------- ASB / illegal use / prohibited conduct ----------
    private boolean hasAsbGround;
    private boolean showAsbSection;
    private String asbAllegedYesNo;
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
    private boolean hasRentArrearsGround;
    private String rentAmount;
    private String rentCalculatedDescription;
    private String rentArrearsTotal;
    private String hasPreviousStepsYesNo;
    private boolean showPreviousStepsFreeText;
    private String previousStepsFreeText;
    private String judgmentRequestedYesNo;

    // ---------- Pre-action ----------
    private String preActionProtocolFollowedYesNo;
    private boolean showPreActionProtocolNotFollowedReason;
    private String preActionProtocolNotFollowedReason;
    private String mediationAttemptedYesNo;
    private String settlementAttemptedYesNo;

    // ---------- Notice ----------
    private String noticeServedYesNo;
    private boolean noticeServedYes;
    private boolean noticeNotServedDisplayed;
    private String noticeNotServedReason;
    private String noticeType;
    private boolean showNoticeType;
    private NoticeServiceMethod methodOfService;
    private String methodOfServiceLabel;
    private String noticeServedOn;
    private boolean showNoticeServedOn;
    private boolean showNoticeServedTime;
    private boolean showNoticeLeftWithName;
    private boolean showNoticeServedToEmail;
    private boolean showNoticeOtherElectronicDetails;
    private boolean showNoticeOtherMeansDetails;
    private String noticeServedTime;
    // Only one detail field is set per render, chosen by methodOfService.
    private String noticeLeftWithName;
    private String noticeServedToEmail;
    private String noticeOtherElectronicDetails;
    private String noticeOtherMeansDetails;
    private boolean showNoticeUploadQuestion;   // No source yet; always false, so the upload rows stay hidden.
    private VerticalYesNo noticeUploadedYesNo;
    private boolean noticeUploadedYes;
    private boolean noticeUploadedNo;
    private String noticeNotUploadedReason;     // No source yet; null until the source field is identified.

    // ---------- Tenancy / licence ----------
    private String tenancyTypeLabel;
    private String tenancyStartDate;
    private boolean showTenancyStartDate;
    private VerticalYesNo tenancyUploadedYesNo;
    private boolean showTenancyUploadedQuestion;
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
    private String hasUnderlesseeYesNo;
    private List<ClaimFormUnderlesseeRow> underlessees;

    // ---------- Demotion of tenancy ----------
    private boolean showIsDemotionClaim;
    private String isDemotionClaimYesNo;
    private boolean showDemotionDetails;
    private String demotionHousingActSection;
    private String hasServedDemotionTermsYesNo;
    private boolean showDemotionTermsFreeText;
    private String demotionTermsFreeText;
    private String demotionReasonsFreeText;

    // ---------- Suspension of right to buy ----------
    private boolean showIsSuspensionClaim;
    private String isSuspensionClaimYesNo;
    private boolean showSuspensionDetails;
    private String suspensionHousingActSection;
    private String suspensionReasonsFreeText;

    // ---------- PCSC (Wales) ----------
    private boolean showPcscSection;
    private String isPcscYesNo;
    private boolean showPcscDetails;
    private String pcscTermsAgreedYesNo;
    private boolean showPcscTermsFreeText;
    private String pcscTermsFreeText;
    private String pcscReasonFreeText;

    // ---------- Required documents (Wales) ----------
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
    private boolean signedByLegalRep;
    private boolean signedByClaimant;
    /**
     * The "Full name" / "Position" labels are static text in the template, so these fields carry
     * only the values. The template must render them as a two-column label|value row (like every
     * other answer row) or the values render bare with no labels.
     */
    private String sotFullName;
    private String sotFirmName;
    private String sotPositionHeld;

}
