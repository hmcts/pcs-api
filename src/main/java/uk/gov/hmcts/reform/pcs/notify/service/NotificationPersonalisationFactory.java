package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersonalisationFactory {

    private final PartyService partyService;

    public BasePersonalisation forDefendant(DefendantResponseEntity defendantResponse) {
        PartyEntity defendant = defendantResponse.getParty();

        return buildPersonalisation(defendant, defendantResponse.getPcsCase());
    }

    public BasePersonalisation forClaimant(ClaimEntity claim) {
        return buildPersonalisation(partyService.getPrimaryClaimantPartyEntity(claim.getPcsCase()), claim.getPcsCase());
    }

    public ClaimantBasePersonalisation forClaimant(long caseReference, PCSCase pcsCase) {
        String toLineClaimantName = getClaimantName(pcsCase.getClaimantInformation());
        String claimantNameUpper = toLineClaimantName.toUpperCase(Locale.ROOT);

        DefendantDetails primaryDefendantDetails = pcsCase.getDefendant1();

        boolean isNameKnown = primaryDefendantDetails.getNameKnown() != null
            && primaryDefendantDetails.getNameKnown().toBoolean();
        String primaryDefendantName = getDefendantName(
            isNameKnown,
            primaryDefendantDetails.getFirstName(),
            primaryDefendantDetails.getLastName());

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

    private BasePersonalisation buildPersonalisation(
        PartyEntity emailRecipient,
        PcsCaseEntity pcsCaseEntity
    ) {
        PartyEntity primaryClaimant = partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);
        PartyEntity primaryDefendant = partyService.getPrimaryDefendantPartyEntity(pcsCaseEntity);

        String claimantName = primaryClaimant.getOrgName() != null
            ? primaryClaimant.getOrgName().toUpperCase(Locale.ROOT)
            : formatNameUpperForNotification(primaryClaimant.getFirstName(), primaryClaimant.getLastName());

        String primaryDefendantName = getDefendantName(
            primaryDefendant.getNameKnown() != null && primaryDefendant.getNameKnown().toBoolean(),
            primaryDefendant.getFirstName(),
            primaryDefendant.getLastName());

        return BasePersonalisation.builder()
            .firstName(emailRecipient.getFirstName() != null
                           ? emailRecipient.getFirstName() : emailRecipient.getOrgName())
            .lastName(emailRecipient.getLastName() != null
                          ? emailRecipient.getLastName() : "")
            .caseNumber(formatCaseReference(pcsCaseEntity.getCaseReference().toString()))
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

    private static String getDefendantName(boolean isNameKnown, String firstName, String lastName) {
        return isNameKnown && firstName != null && lastName != null
            ? formatNameUpperForNotification(firstName, lastName)
            : "PERSONS UNKNOWN";
    }

    private static String formatNameUpperForNotification(String firstName, String lastName) {
        return String.format("%s %s", firstName, lastName).toUpperCase(Locale.ROOT);
    }

    public static String formatCaseReference(String caseReference) {
        if (caseReference == null) {
            return null;
        }

        return caseReference.replaceAll("(.{4})(?!$)", "$1-");
    }
}
