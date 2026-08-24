package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.OrganisationBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentRequiredPersonalisation;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersonalisationFactory {

    private final PartyService partyService;

    @Value("${frontend.url}")
    private String frontendUrl;

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
            primaryDefendantDetails.getLastName()
        );

        return ClaimantBasePersonalisation.builder()
            .toLineClaimantName(toLineClaimantName)
            .caseNumber(formatCaseReference(Long.toString(caseReference)))
            .claimantName(claimantNameUpper)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    public BasePersonalisation forParty(PartyEntity partyEntity, PcsCaseEntity pcsCaseEntity) {
        return buildPersonalisation(partyEntity, pcsCaseEntity);
    }

    public OrganisationBasePersonalisation forOrganisation(
        OrganisationEntity organisationEntity, PcsCaseEntity pcsCaseEntity) {
        return buildPersonalisation(pcsCaseEntity, organisationEntity);
    }

    public CounterclaimPaymentSuccessPersonalisation counterclaimSuccess(DefendantResponseEntity defendantResponse,
                                                                         String paymentReference) {
        return CounterclaimPaymentSuccessPersonalisation.builder()
            .base(forDefendant(defendantResponse))
            .paymentReferenceNumber(paymentReference)
            .build();
    }

    public CounterclaimPaymentRequiredPersonalisation counterclaimPaymentRequired(
        OrganisationEntity organisationEntity, PcsCaseEntity pcsCaseEntity
    ) {
        String caseRef = pcsCaseEntity.getCaseReference().toString();
        String paymentUrl = String.format(
            ("%s/case/%s/respond-to-claim/counter-claim-application-fee-amount"),
            frontendUrl,
            caseRef
        );

        return CounterclaimPaymentRequiredPersonalisation.builder()
            .base(forOrganisation(organisationEntity, pcsCaseEntity))
            .paymentUrl(paymentUrl)
            .build();
    }

    public CounterclaimPaymentRequiredPersonalisation counterclaimPaymentRequired(
        DefendantResponseEntity defendantResponse
    ) {
        String paymentUrl = Optional.ofNullable(defendantResponse)
            .map(DefendantResponseEntity::getPcsCase)
            .map(PcsCaseEntity::getCaseReference)
            .map(Object::toString)
            .map(caseRef -> String.format(
                ("%s/case/%s/respond-to-claim/counter-claim-application-fee-amount"),
                frontendUrl,
                caseRef
            ))
            .orElse(null);

        return CounterclaimPaymentRequiredPersonalisation.builder()
            .base(forDefendant(defendantResponse))
            .paymentUrl(paymentUrl)
            .build();
    }

    private OrganisationBasePersonalisation buildPersonalisation(
        PcsCaseEntity pcsCaseEntity,
        OrganisationEntity organisationEntity
    ) {
        PartyEntity primaryClaimant = partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);
        PartyEntity primaryDefendant = partyService.getPrimaryDefendantPartyEntity(pcsCaseEntity);

        String claimantName = getClaimantName(primaryClaimant);
        String primaryDefendantName = getPrimaryDefendantName(primaryDefendant);

        return OrganisationBasePersonalisation.builder()
            .organisationName(organisationEntity.getOrganisationName())
            .caseNumber(formatCaseReference(pcsCaseEntity.getCaseReference().toString()))
            .claimantName(claimantName)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    private BasePersonalisation buildPersonalisation(
        PartyEntity emailRecipient,
        PcsCaseEntity pcsCaseEntity
    ) {
        PartyEntity primaryClaimant = partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);
        PartyEntity primaryDefendant = partyService.getPrimaryDefendantPartyEntity(pcsCaseEntity);

        String claimantName = getClaimantName(primaryClaimant);
        String primaryDefendantName = getPrimaryDefendantName(primaryDefendant);

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


    private String getClaimantName(PartyEntity primaryClaimant) {
        return primaryClaimant.getOrgName() != null
            ? primaryClaimant.getOrgName().toUpperCase(Locale.ROOT)
            : formatNameUpperForNotification(primaryClaimant.getFirstName(), primaryClaimant.getLastName());
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

    private String getPrimaryDefendantName(PartyEntity primaryDefendant) {
        return getDefendantName(
            primaryDefendant.getNameKnown() != null && primaryDefendant.getNameKnown().toBoolean(),
            primaryDefendant.getFirstName(),
            primaryDefendant.getLastName()
        );
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
