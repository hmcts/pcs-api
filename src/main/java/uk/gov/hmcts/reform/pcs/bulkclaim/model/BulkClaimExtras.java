package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PCOL data that PCS's own claim model does not hold, staged as JSON and persisted alongside the claim.
 * Defendant extras are in the same order as the defendants on the staged PCSCase (defendant1 first).
 */
public record BulkClaimExtras(
    String schemaVersion,
    String claimantDesignation,
    String legacyPcolClaimantReference,
    String legacyPcolSolicitorReference,
    String solicitorFirmName,
    Boolean claimingCosts,
    String noticeType,
    PartyExtras claimant,
    List<PartyExtras> defendants,
    List<PaymentHistoryLine> paymentHistory,
    MortgageParticulars mortgageParticulars,
    List<Occupant> occupants,
    Boolean noticeOfActingRequired
) {

    public record PartyExtras(
        String title,
        String initials,
        String otherNames,
        String dxNumber,
        String faxNumber,
        Boolean isOccupant,
        String phoneNumber,
        String emailAddress
    ) {
    }

    public record PaymentHistoryLine(
        LocalDate paymentDueDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal cumulativeArrears
    ) {
    }

    public record Occupant(String title, String firstName, String lastName, String otherNames, String initials) {
    }

    public record AdditionalPayment(BigDecimal amount, String reason, BigDecimal arrears) {
    }

    public record MortgageParticulars(
        LocalDate mortgageDate,
        LocalDate defaultNoticeDate,
        BigDecimal loanAmount,
        String currentRepaymentTerms,
        LocalDate fullRepaymentDate,
        BigDecimal fullRepaymentAmount,
        BigDecimal costsAndAdministration,
        BigDecimal outstandingAmount,
        BigDecimal startInterestRate,
        BigDecimal preArrearsInterestRate,
        BigDecimal claimStartInterestRate,
        String interestedParty,
        LocalDate tenancyDate,
        LocalDate tenantNoticeServedDate,
        BigDecimal totalArrears,
        String recoverySteps,
        Boolean moneyJudgmentWanted,
        String originalProviderName,
        LocalDate originalProviderBoughtDate,
        LocalDate originalProviderDefendantNotifiedDate,
        List<AdditionalPayment> additionalPayments
    ) {
    }
}
