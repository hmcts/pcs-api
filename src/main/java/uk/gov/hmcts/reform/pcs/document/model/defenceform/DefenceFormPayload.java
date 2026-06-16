package uk.gov.hmcts.reform.pcs.document.model.defenceform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.time.LocalDate;
import java.util.List;

/**
 * Typed payload rendered into the defence form Docmosis template
 * ({@code CV-PCS-DEF-ENG-Defence-Form.docx}). One form is produced for the defendant who submits
 * the response.
 *
 * <p>Values are pre-formatted by {@code DefenceFormPayloadBuilder}, except the two title-block dates
 * ({@code issueDateSealed}, {@code submittedOn}) which stay {@link LocalDate} and are formatted in
 * the template. The {@code show*} flags gate the matching template rows/sections so blank optional
 * answers are hidden rather than rendered empty.</p>
 */
@Data
@Builder
public class DefenceFormPayload implements FormPayload {

    // ---------- Title block ----------
    private String referenceNumber;
    private String caseName;
    private LocalDate issueDateSealed;
    private LocalDate submittedOn;
    /**
     * {@code @JsonProperty} keeps the wire key as {@code isWales}: Jackson otherwise strips the
     * {@code is} prefix, but the template tag {@code <<cs_isWales>>} needs the key to stay
     * {@code isWales}.
     */
    @JsonProperty("isWales")
    private boolean isWales;
    // Complement of isWales; Docmosis compact syntax can't negate, so we send both.
    @JsonProperty("isEngland")
    private boolean isEngland;

    // ---------- Claimant ----------
    private String claimantName;
    private ClaimFormAddress claimantAddress;
    private boolean hasClaimantAddressLine2;
    private boolean hasClaimantAddressLine3;
    private boolean hasClaimantCounty;

    // ---------- Defendant ----------
    private String defendantName;
    private ClaimFormAddress defendantAddress;
    private boolean hasDefendantAddressLine2;
    private boolean hasDefendantAddressLine3;
    private boolean hasDefendantCounty;

    // ---------- Response to the claim ----------
    private String tenancyTypeConfirmation;
    private boolean showCorrectedTenancyType;
    private String correctedTenancyType;
    private String tenancyStartDateConfirmation;
    private boolean showCorrectedStartDate;
    private String correctedStartDate;
    private boolean showLandlordRegistered;
    private String landlordRegistered;
    private boolean showLandlordLicensed;
    private String landlordLicensed;
    private boolean showWrittenTerms;
    private String writtenTerms;
    private String possessionNoticeReceived;
    private boolean showNoticeReceivedDate;
    private String noticeReceivedDate;
    private boolean showRentArrearsAmountQuestion;
    private String rentArrearsAmountConfirmation;
    private boolean showAdmittedArrearsAmount;
    private String admittedArrearsAmount;
    private boolean showDisputeOtherParts;
    private String disputeClaim;
    private boolean showDisputeClaimDetails;
    private String disputeClaimDetails;

    // ---------- Payments or agreements (rent-arrears claims only) ----------
    private boolean showPaymentsSection;
    private String anyPaymentsMade;
    private boolean showPaymentDetails;
    private String paymentDetails;
    private String repaymentPlanAgreed;
    private boolean showRepaymentAgreedDetails;
    private String repaymentAgreedDetails;
    private boolean showOfferInstalments;
    private String repayArrearsInstalments;
    private boolean showInstalmentAmount;
    private String instalmentAmount;
    private String instalmentFrequency;

    // ---------- Household & circumstances ----------
    private String dependantChildren;
    private boolean showDependantChildrenDetails;
    private String dependantChildrenDetails;
    private String otherDependants;
    private boolean showOtherDependantsDetails;
    private String otherDependantsDetails;
    private String otherTenants;
    private boolean showOtherTenantsDetails;
    private String otherTenantsDetails;
    private String alternativeAccommodation;
    private boolean showTransferDate;
    private String transferDate;
    private String shareAdditionalCircumstances;
    private boolean showAdditionalCircumstancesDetails;
    private String additionalCircumstancesDetails;
    private String exceptionalHardship;
    private boolean showExceptionalHardshipDetails;
    private String exceptionalHardshipDetails;

    // ---------- Income & expenses ----------
    private boolean showIncomeExpenseSection;
    private List<DefenceFormAmountRow> income;
    private boolean showMoneyFromElsewhere;
    private String moneyFromElsewhereDetails;
    private boolean showUcApplicationDate;
    private String ucApplicationDate;
    private String priorityDebts;
    private boolean showDebtDetails;
    private String debtTotal;
    private String debtContribution;
    private String debtContributionFrequency;
    private List<DefenceFormAmountRow> expenses;

    // ---------- Additional information ----------
    private String otherConsiderations;
    private boolean showAdditionalInfoDetails;
    private String otherConsiderationsDetails;

    // ---------- Statement of truth ----------
    private String sotFullName;

}
