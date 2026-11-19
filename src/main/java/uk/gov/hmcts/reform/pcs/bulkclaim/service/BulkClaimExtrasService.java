package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.AdditionalPayment;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.MortgageParticulars;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.Occupant;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PartyExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PaymentHistoryLine;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimOccupantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimPaymentHistoryLineEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageAdditionalPaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Comparator;
import java.util.List;

/**
 * Persists the PCOL-only data of a bulk claim onto the claim and parties created from the staged case.
 */
@Service
@RequiredArgsConstructor
public class BulkClaimExtrasService {

    private final PcsCaseService pcsCaseService;

    public void apply(long caseReference, BulkClaimExtras extras) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getMainClaim();
        if (claim == null) {
            throw new IllegalStateException("Case %d has no main claim to apply bulk claim data to"
                                                .formatted(caseReference));
        }

        claim.setSubmissionChannel(SubmissionChannel.BULK_API);
        claim.setClaimantDesignation(extras.claimantDesignation());
        claim.setLegacyPcolClaimantReference(extras.legacyPcolClaimantReference());
        claim.setLegacyPcolSolicitorReference(extras.legacyPcolSolicitorReference());
        claim.setSolicitorFirmName(extras.solicitorFirmName());
        claim.setClaimCosts(yesOrNo(extras.claimingCosts()));

        applyPartyExtras(claim, extras);
        applyPaymentHistory(claim, extras.paymentHistory());
        applyMortgageParticulars(claim, extras.mortgageParticulars());
        applyOccupants(claim, extras.occupants());
    }

    private void applyPartyExtras(ClaimEntity claim, BulkClaimExtras extras) {
        List<PartyEntity> claimants = partiesWithRole(claim, PartyRole.CLAIMANT);
        if (extras.claimant() != null && !claimants.isEmpty()) {
            applyTo(claimants.getFirst(), extras.claimant());
        }

        List<PartyEntity> defendants = partiesWithRole(claim, PartyRole.DEFENDANT);
        List<PartyExtras> defendantExtras = extras.defendants();
        if (defendantExtras == null) {
            return;
        }
        if (defendantExtras.size() > defendants.size()) {
            throw new IllegalStateException("Staged extras describe %d defendants but the claim has %d"
                                                .formatted(defendantExtras.size(), defendants.size()));
        }
        for (int i = 0; i < defendantExtras.size(); i++) {
            applyTo(defendants.get(i), defendantExtras.get(i));
        }
    }

    private static List<PartyEntity> partiesWithRole(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .sorted(Comparator.comparing(ClaimPartyEntity::getRank))
            .map(ClaimPartyEntity::getParty)
            .toList();
    }

    private static void applyTo(PartyEntity party, PartyExtras extras) {
        party.setTitle(extras.title());
        party.setInitials(extras.initials());
        party.setOtherNames(extras.otherNames());
        party.setDxNumber(extras.dxNumber());
        party.setFaxNumber(extras.faxNumber());
        party.setIsOccupant(yesOrNo(extras.isOccupant()));
    }

    private static void applyPaymentHistory(ClaimEntity claim, List<PaymentHistoryLine> lines) {
        if (lines == null) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            PaymentHistoryLine line = lines.get(i);
            claim.addPaymentHistoryLine(ClaimPaymentHistoryLineEntity.builder()
                .rank(i + 1)
                .paymentDueDate(line.paymentDueDate())
                .amountDue(line.amountDue())
                .amountPaid(line.amountPaid())
                .cumulativeArrears(line.cumulativeArrears())
                .build());
        }
    }

    private static void applyMortgageParticulars(ClaimEntity claim, MortgageParticulars particulars) {
        if (particulars == null) {
            return;
        }
        MortgageParticularsEntity entity = MortgageParticularsEntity.builder()
            .mortgageDate(particulars.mortgageDate())
            .defaultNoticeDate(particulars.defaultNoticeDate())
            .loanAmount(particulars.loanAmount())
            .currentRepaymentTerms(particulars.currentRepaymentTerms())
            .fullRepaymentDate(particulars.fullRepaymentDate())
            .fullRepaymentAmount(particulars.fullRepaymentAmount())
            .costsAndAdministration(particulars.costsAndAdministration())
            .outstandingAmount(particulars.outstandingAmount())
            .startInterestRate(particulars.startInterestRate())
            .preArrearsInterestRate(particulars.preArrearsInterestRate())
            .claimStartInterestRate(particulars.claimStartInterestRate())
            .interestedParty(particulars.interestedParty())
            .tenancyDate(particulars.tenancyDate())
            .tenantNoticeServedDate(particulars.tenantNoticeServedDate())
            .totalArrears(particulars.totalArrears())
            .recoverySteps(particulars.recoverySteps())
            .moneyJudgmentWanted(yesOrNo(particulars.moneyJudgmentWanted()))
            .originalProviderName(particulars.originalProviderName())
            .originalProviderBoughtDate(particulars.originalProviderBoughtDate())
            .originalProviderDefendantNotifiedDate(particulars.originalProviderDefendantNotifiedDate())
            .build();

        List<AdditionalPayment> additionalPayments = particulars.additionalPayments();
        if (additionalPayments != null) {
            for (int i = 0; i < additionalPayments.size(); i++) {
                AdditionalPayment payment = additionalPayments.get(i);
                entity.addAdditionalPayment(MortgageAdditionalPaymentEntity.builder()
                    .rank(i + 1)
                    .amount(payment.amount())
                    .reason(payment.reason())
                    .arrears(payment.arrears())
                    .build());
            }
        }
        claim.setMortgageParticulars(entity);
    }

    private static void applyOccupants(ClaimEntity claim, List<Occupant> occupants) {
        if (occupants == null) {
            return;
        }
        for (int i = 0; i < occupants.size(); i++) {
            Occupant occupant = occupants.get(i);
            claim.addOccupant(ClaimOccupantEntity.builder()
                .rank(i + 1)
                .title(occupant.title())
                .firstName(occupant.firstName())
                .lastName(occupant.lastName())
                .otherNames(occupant.otherNames())
                .initials(occupant.initials())
                .build());
        }
    }

    private static YesOrNo yesOrNo(Boolean value) {
        return value == null ? null : YesOrNo.from(value);
    }
}
