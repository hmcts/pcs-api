package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisationLegalRep;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.OrganisationBasePersonalisation;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentRequiredPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.NoticeOfChangeCompleteLegalRepPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.NoticeOfChangeCompletedPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.NoticeOfChangeNoLongerRepresentingPersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.MakeAClaimBasePersonalisation;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersonalisationFactory {

    private final PartyService partyService;
    private final AddressFormatter addressFormatter;
    private final AddressMapper addressMapper;
    private final CaseNameFormatter caseNameFormatter;

    @Value("${frontend.url}")
    private String frontendUrl;

    public BasePersonalisation forDefendant(DefendantResponseEntity defendantResponse) {
        PartyEntity defendant = defendantResponse.getParty();

        return buildPersonalisation(defendant, defendantResponse.getPcsCase());
    }

    public MakeAClaimBasePersonalisation forClaimant(ClaimEntity claim) {
        String nextStepUrl = Optional.of(claim.getPcsCase())
            .map(PcsCaseEntity::getCaseReference)
            .map(Object::toString)
            .map(caseRef -> String.format(
                ("%s/cases/case-details/PCS/PCS/%s#Case%%20Parties"),
                frontendUrl,
                caseRef
            ))
            .orElse(null);

        return MakeAClaimBasePersonalisation.builder()
            .base(buildPersonalisation(
                    partyService.getPrimaryClaimantPartyEntity(claim.getPcsCase()),
                    claim.getPcsCase()))
            .nextStepUrl(nextStepUrl)
            .build();
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

        String nextStepUrl = String.format("%s/cases/case-details/PCS/PCS/%s#Next%%20steps",
                                            frontendUrl,
                                            caseReference);

        return ClaimantBasePersonalisation.builder()
            .toLineClaimantName(toLineClaimantName)
            .caseNumber(formatCaseReference(Long.toString(caseReference)))
            .caseName(caseNameFormatter.formatCaseName(pcsCase))
            .claimantName(claimantNameUpper)
            .primaryDefendantName(primaryDefendantName)
            .nextStepUrl(nextStepUrl)
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
        String paymentUrl = Optional.of(pcsCaseEntity)
            .map(PcsCaseEntity::getCaseReference)
            .map(Object::toString)
            .map(caseRef -> String.format(
                ("%s/cases/case-details/PCS/PCS/%s#Service%%20Request"),
                frontendUrl,
                caseRef
            ))
            .orElse(null);

        return CounterclaimPaymentRequiredPersonalisation.builder()
            .base(forOrganisation(organisationEntity, pcsCaseEntity))
            .paymentUrl(paymentUrl)
            .build();
    }

    public CounterclaimPaymentRequiredPersonalisation counterclaimPaymentRequired(
        DefendantResponseEntity defendantResponse
    ) {
        String paymentUrl = String.format("%s/claims", frontendUrl);

        return CounterclaimPaymentRequiredPersonalisation.builder()
            .base(forDefendant(defendantResponse))
            .paymentUrl(paymentUrl)
            .build();
    }

    public CounterclaimPaymentSuccessPersonalisationLegalRep counterclaimSuccessOrganisation(
        DefendantResponseEntity defendantResponse, String paymentReference,
        OrganisationEntity legalRepresentativeOrganisationEntity
    ) {

        return  CounterclaimPaymentSuccessPersonalisationLegalRep.builder()
            .base(buildPersonalisation(defendantResponse.getPcsCase(),legalRepresentativeOrganisationEntity))
            .paymentReferenceNumber(paymentReference)
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
            .caseName(getFormattedCaseName(pcsCaseEntity))
            .claimantName(claimantName)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    private BasePersonalisation buildPersonalisation(
        PartyEntity emailRecipient,
        PcsCaseEntity pcsCaseEntity
    ) {
        return buildPersonalisation(
            emailRecipient.getFirstName() != null ? emailRecipient.getFirstName() : emailRecipient.getOrgName(),
            Objects.toString(emailRecipient.getLastName(), ""),
            pcsCaseEntity
        );
    }

    private BasePersonalisation buildPersonalisation(
        String recipientFirstName,
        String recipientLastName,
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
            .firstName(recipientFirstName)
            .lastName(recipientLastName)
            .caseNumber(formatCaseReference(pcsCaseEntity.getCaseReference().toString()))
            .caseName(getFormattedCaseName(pcsCaseEntity))
            .claimantName(claimantName)
            .primaryDefendantName(primaryDefendantName)
            .build();
    }

    public NoticeOfChangeCompletedPersonalisation noticeOfChangeCompleted(PartyEntity partyEntity,
                                                                         PcsCaseEntity pcsCaseEntity) {

        return NoticeOfChangeCompletedPersonalisation.builder()
            .base(buildPersonalisation(partyEntity, pcsCaseEntity))
            .address(formatPropertyAddress(pcsCaseEntity))
            .build();
    }

    public NoticeOfChangeNoLongerRepresentingPersonalisation noticeOfChangeNoLongerRepresenting(
        OrganisationEntity legalRepresentativeOrganisation,
        PcsCaseEntity pcsCaseEntity
    ) {
        String organisationName = legalRepresentativeOrganisation.getOrganisationName();

        return NoticeOfChangeNoLongerRepresentingPersonalisation.builder()
            .base(buildPersonalisation("", "", pcsCaseEntity))
            .organisationName(organisationName != null ? organisationName : "")
            .build();
    }

    public NoticeOfChangeCompleteLegalRepPersonalisation noticeOfChangeCompleteLegalRep(
        OrganisationEntity legalRepresentativeOrganisation,
        PartyEntity representedDefendant
    ) {
        String organisationName = legalRepresentativeOrganisation.getOrganisationName();

        return NoticeOfChangeCompleteLegalRepPersonalisation.builder()
            .base(buildPersonalisation("", "", representedDefendant.getPcsCase()))
            .organisationName(organisationName != null ? organisationName : "")
            .partyName(getDefendantName(
                representedDefendant.getNameKnown() != null && representedDefendant.getNameKnown().toBoolean(),
                representedDefendant.getFirstName(),
                representedDefendant.getLastName()))
            .build();
    }

    private String formatPropertyAddress(PcsCaseEntity pcsCaseEntity) {
        AddressEntity propertyAddress = pcsCaseEntity.getPropertyAddress();

        if (propertyAddress == null) {
            return "";
        }

        return addressFormatter.formatFullAddressWithoutCountry(
            addressMapper.toAddressUK(propertyAddress),
            AddressFormatter.COMMA_DELIMITER
        );
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

    public String getFormattedCaseName(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity claim = pcsCaseEntity.getClaims().getFirst();

        List<Party> claimants = PartyDisplayMapper.partiesByRole(claim, PartyRole.CLAIMANT).stream()
            .map(PartyDisplayMapper::toDomainParty).toList();
        List<Party> defendants = PartyDisplayMapper.partiesByRole(claim, PartyRole.DEFENDANT).stream()
            .map(PartyDisplayMapper::toDomainParty).toList();

        return caseNameFormatter.formatCaseName(claimants, defendants);
    }
}
