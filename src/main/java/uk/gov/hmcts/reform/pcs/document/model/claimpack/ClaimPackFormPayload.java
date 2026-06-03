package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
     */
    private boolean isWales;

    // ---------- §6.3.2 Claimant ----------
    private ClaimPackParty claimant;
    /** Wales-only row in §6.3.2. */
    private VerticalYesNo claimantIsExemptLandlord;

    // ---------- §6.3.3 – §6.3.6 Defendants ----------
    private ClaimPackParty defendant1;
    private List<ClaimPackParty> additionalDefendants;

    // ---------- §6.3.7 Claim details / grounds ----------
    private ClaimPackAddress propertyAddress;
    /** Drives R-TENANCY-INTRO-DEMOTED-OTHER. */
    private boolean isIntroDemotedOtherTenancy;
    private VerticalYesNo hasGroundsYesNo;
    private List<ClaimPackGround> grounds;
    /** Drives R-GROUND-OTHER. */
    private boolean hasOtherGround;
    private String otherGroundsDescription;
    /** Drives R-NO-OR-ABSOLUTE-OR-OTHER-GROUNDS. */
    private boolean isNoOrAbsoluteOrOtherGrounds;
    /** §13.3 gap (2) — null until source field is identified. */
    private String whyClaimingPossession;
    private VerticalYesNo hasAdditionalReasonsYesNo;
    /** Drives R-ADDITIONAL-REASONS-PROVIDED. */
    private boolean additionalReasonsProvided;
    private String additionalReasonsFreeText;

    // ---------- §6.3.8 ASB / illegal use / prohibited conduct ----------
    /** Combined with isWales gates §6.3.8 — R-WALES-ASB-GROUND. */
    private boolean hasAsbGround;
    private VerticalYesNo asbAllegedYesNo;
    private String asbDetailsFreeText;
    private VerticalYesNo illegalUseAllegedYesNo;
    private String illegalUseDetailsFreeText;
    private VerticalYesNo otherProhibitedAllegedYesNo;
    private String otherProhibitedDetailsFreeText;

    // ---------- §6.3.9 Rent arrears ----------
    /** Drives R-RENT-ARREARS-GROUND (whole-section gate). */
    private boolean hasRentArrearsGround;
    private BigDecimal rentAmount;
    private String rentCalculatedDescription;
    private BigDecimal rentArrearsTotal;
    private VerticalYesNo hasPreviousStepsYesNo;
    private String previousStepsFreeText;
    private VerticalYesNo judgmentRequestedYesNo;

    // ---------- §6.3.10 Pre-action ----------
    private VerticalYesNo preActionProtocolFollowedYesNo;
    private String preActionProtocolNotFollowedReason;
    private VerticalYesNo mediationAttemptedYesNo;
    private VerticalYesNo settlementAttemptedYesNo;

    // ---------- §6.3.11 Notice ----------
    private VerticalYesNo noticeServedYesNo;
    /** Wales-no-notice OR England-no-notice — derived in builder. */
    private boolean noticeNotServedDisplayed;
    private String noticeNotServedReason;
    /** Wales-only. */
    private String noticeType;
    /** Used to drive R-METHOD-* gates inside the template. */
    private NoticeServiceMethod methodOfService;
    private String methodOfServiceLabel;
    /** Derived: servingMethod ∈ {PERSONALLY_HANDED, EMAIL, OTHER_ELECTRONIC, OTHER}. */
    private boolean methodRequiresTime;
    private LocalDate noticeServedOn;
    private LocalTime noticeServedTime;
    /** Four shared-storage detail fields — only one is populated per render, driven by methodOfService. */
    private String noticeLeftWithName;
    private String noticeServedToEmail;
    private String noticeOtherElectronicDetails;
    private String noticeOtherMeansDetails;
    private VerticalYesNo noticeUploadedYesNo;
    /** §13.3 gap (3) — null until source field is identified. */
    private String noticeNotUploadedReason;

    // ---------- §6.3.12 Tenancy / licence ----------
    private String tenancyTypeLabel;
    private LocalDate tenancyStartDate;
    /** England-only row. */
    private VerticalYesNo tenancyUploadedYesNo;
    /** England-only row. */
    private String tenancyNotUploadedReason;

    // ---------- §6.3.13 / §6.3.14 Circumstances ----------
    private VerticalYesNo hasClaimantCircsYesNo;
    private String claimantCircsFreeText;
    private VerticalYesNo hasDefendantCircsYesNo;
    private String defendantCircsFreeText;

    // ---------- §6.3.15 Underlessees / mortgagees ----------
    private VerticalYesNo hasUnderlesseeYesNo;
    private ClaimPackParty underlessee1;
    private List<ClaimPackParty> additionalUnderlessees;

    // ---------- §6.3.16 Demotion of tenancy ----------
    private VerticalYesNo isDemotionClaimYesNo;
    private String demotionHousingActSection;
    private VerticalYesNo hasServedDemotionTermsYesNo;
    private String demotionTermsFreeText;
    private String demotionReasonsFreeText;

    // ---------- §6.3.17 Suspension of right to buy ----------
    private VerticalYesNo isSuspensionClaimYesNo;
    private String suspensionHousingActSection;
    private String suspensionReasonsFreeText;

    // ---------- §6.3.18 PCSC (Wales) ----------
    private VerticalYesNo isPcscYesNo;
    private VerticalYesNo pcscTermsAgreedYesNo;
    private String pcscTermsFreeText;
    private String pcscReasonFreeText;

    // ---------- §6.3.19 Required documents (Wales) — §13.3 gap (4) ----------
    private VerticalYesNo epcUploadedYesNo;
    private String epcNotUploadedReason;
    private VerticalYesNo gasSafetyUploadedYesNo;
    private String gasSafetyNotUploadedReason;
    private VerticalYesNo eicrUploadedYesNo;
    private String eicrNotUploadedReason;

    // ---------- §6.3.20 / §6.3.21 Statement of truth ----------
    /** false = claimant signs (§6.3.20), true = legal rep (§6.3.21). */
    private boolean signedByLegalRep;
    private String sotFullName;
    /** Legal-rep variant only. */
    private String sotFirmName;
    private String sotPositionHeld;

}
