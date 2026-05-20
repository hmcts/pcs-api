package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;

import java.util.Locale;

@Slf4j
@Service
public class NotificationPersonalisationFactory {
    private static final String NO_CLAIMANT_PARTY_FOUND_MSG = "No claimant party found for defendant response: %s";

    public BasePersonalisation forDefendant(DefendantResponseEntity defendantResponse) {
        PartyEntity defendant = defendantResponse.getParty();
        PartyEntity claimant = defendantResponse.getClaim().getClaimantParty();

        if (claimant == null) {
            throw new PartyNotFoundException(String.format(NO_CLAIMANT_PARTY_FOUND_MSG, defendantResponse.getId()));
        }

        return buildPersonalisation(defendant, claimant, defendant, defendantResponse.getPcsCase());
    }

    public BasePersonalisation forClaimant(ClaimEntity claim, PartyEntity claimant) {
        return buildPersonalisation(claimant, claimant, claim.getDefendantParty(), claim.getPcsCase());
    }

    public ClaimantBasePersonalisation forClaimant(long caseReference, PCSCase pcsCase) {
        String toLineClaimantName = getClaimantName(pcsCase.getClaimantInformation());
        String claimantNameUpper = toLineClaimantName.toUpperCase(Locale.ROOT);

        DefendantDetails primaryDefendantDetails = pcsCase.getDefendant1();
        String primaryDefendantName = formatNameUpperForNotification(
            primaryDefendantDetails.getFirstName(), primaryDefendantDetails.getLastName()
        );

        return ClaimantBasePersonalisation.builder()
            .toLineClaimantName(toLineClaimantName)
            .caseNumber(formatCaseReference(Long.toString(caseReference)))
            .claimantName(claimantNameUpper)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    public CounterclaimPaymentSuccessPersonalisation counterclaimSuccess(DefendantResponseEntity defendantResponse) {
        FeePaymentEntity defendantFeePayment = defendantResponse.getClaim().getFeePayment();
        if (defendantFeePayment == null || !defendantFeePayment.getPaymentStatus().equals(PaymentStatus.PAID)) {
            throw new FeePaymentNotFoundException(
                "Paid fee payment not found for defendant response: " + defendantResponse.getId());
        }

        return CounterclaimPaymentSuccessPersonalisation.builder()
            .base(forDefendant(defendantResponse))
            .paymentReferenceNumber(defendantFeePayment.getExternalReference())
            .build();
    }

    private static BasePersonalisation buildPersonalisation(
        PartyEntity emailRecipient,
        PartyEntity claimant,
        PartyEntity defendant,
        PcsCaseEntity pcsCase
    ) {
        String claimantName = claimant.getOrgName() != null
            ? claimant.getOrgName().toUpperCase(Locale.ROOT)
            : formatNameUpperForNotification(claimant.getFirstName(), claimant.getLastName());
        String primaryDefendantName = formatNameUpperForNotification(defendant.getFirstName(), defendant.getLastName());

        return BasePersonalisation.builder()
            .firstName(emailRecipient.getFirstName())
            .lastName(emailRecipient.getLastName())
            .caseNumber(formatCaseReference(pcsCase.getCaseReference().toString()))
            .claimantName(claimantName)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    private static String getClaimantName(ClaimantInformation claimantInformation) {
        VerticalYesNo isClaimantNameOverridden = claimantInformation.getIsClaimantNameCorrect();
        return isClaimantNameOverridden == null || isClaimantNameOverridden.toBoolean()
            ? claimantInformation.getClaimantName()
            : claimantInformation.getOverriddenClaimantName();
    }

    private static String formatNameUpperForNotification(String firstName, String lastName) {
        return String.format("%s %s", firstName, lastName).toUpperCase(Locale.ROOT);
    }

    private static String formatCaseReference(String caseReference) {
        if (caseReference == null) {
            return null;
        }

        return caseReference.replaceAll("(.{4})(?!$)", "$1-");
    }
}
