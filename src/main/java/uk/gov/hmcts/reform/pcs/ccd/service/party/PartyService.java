package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@AllArgsConstructor
public class PartyService {

    private static final String PERSON_UNKNOWN_NAME = "Person unknown";

    private final PartyRepository partyRepository;
    private final AddressMapper addressMapper;
    private final OrganisationService organisationService;

    public void createAllParties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity,
                                 String organisationIdForCurrentUser) {
        String orgProfileId = organisationService.getOrgProfileIdForCurrentUser();
        PartyEntity claimant = findInitialisedClaimant(pcsCaseEntity).orElseGet(PartyEntity::new);
        populateClaimant(claimant, pcsCase, organisationIdForCurrentUser, orgProfileId);
        pcsCaseEntity.addParty(claimant);
        claimEntity.addParty(claimant, PartyRole.CLAIMANT);

        List<PartyEntity> defendants = createDefendants(pcsCase);
        defendants.forEach(
            defendant -> {
                pcsCaseEntity.addParty(defendant);
                claimEntity.addParty(defendant, PartyRole.DEFENDANT);
            }
        );

        List<PartyEntity> underlesseeOrMortgagees = createUnderlesseeOrMortgagees(pcsCase);
        underlesseeOrMortgagees.forEach(
            underlesseeOrMortgagee -> {
                pcsCaseEntity.addParty(underlesseeOrMortgagee);
                claimEntity.addParty(underlesseeOrMortgagee, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
            }
        );
    }

    public PartyEntity getPartyEntityByEntityId(UUID entityId, long caseReference) {
        return partyRepository.queryPartyById(entityId, caseReference)
            .orElseThrow(() -> new PartyNotFoundException(
                "No party found for entity ID: " + entityId + " and case reference: " + caseReference));
    }

    public PartyEntity getPartyEntityByIdamId(UUID idamId, long caseReference) {
        return partyRepository.queryPartyByIdamId(idamId, caseReference)
            .orElseThrow(() -> new PartyNotFoundException(
                "No party found for IDAM ID: " + idamId + " and case reference: " + caseReference));
    }

    public String getPartyName(PartyEntity partyEntity) {
        if (partyEntity.getNameKnown() == VerticalYesNo.NO) {
            return PERSON_UNKNOWN_NAME;
        }
        if (StringUtils.isNotBlank(partyEntity.getOrgName())) {
            return partyEntity.getOrgName();
        } else {
            String partyName = Stream.of(partyEntity.getFirstName(), partyEntity.getLastName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));

            return !partyName.isBlank() ? partyName : PERSON_UNKNOWN_NAME;
        }
    }

    public String getPartyLabel(ClaimEntity mainClaim, UUID partyId) {
        ClaimPartyEntity applicantClaimParty = getClaimParty(mainClaim, partyId);

        if (applicantClaimParty.getRole() == PartyRole.CLAIMANT) {
            return "Claimant %d".formatted(applicantClaimParty.getRank());
        } else if (applicantClaimParty.getRole() == PartyRole.DEFENDANT) {
            return "Defendant %d".formatted(applicantClaimParty.getRank());
        } else {
            return null;
        }
    }

    public PartyEntity getPrimaryClaimantPartyEntity(PcsCaseEntity pcsCaseEntity) {
        return getPrimaryPartyEntityOfRole(pcsCaseEntity, PartyRole.CLAIMANT);
    }

    public PartyEntity getPrimaryDefendantPartyEntity(PcsCaseEntity pcsCaseEntity) {
        return getPrimaryPartyEntityOfRole(pcsCaseEntity, PartyRole.DEFENDANT);
    }

    private static PartyEntity getPrimaryPartyEntityOfRole(PcsCaseEntity pcsCaseEntity, PartyRole role) {
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        return mainClaim.getClaimParties().stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == role)
            .findFirst()
            .map(ClaimPartyEntity::getParty)
            .orElseThrow(() -> new PartyNotFoundException("No party of type %s found on case".formatted(role)));
    }

    public boolean canSendEmailNotification(PartyEntity party, PartyRole role) {
        if (party.getEmailAddress() == null) {
            return false;
        }

        if (role == PartyRole.CLAIMANT) {
            return true;
        }

        ContactPreferencesEntity preferences = party.getContactPreferences();
        return preferences != null
            && preferences.getContactByEmail() != null
            && preferences.getContactByEmail().toBoolean();
    }

    public PartyRole getPartyRole(PartyEntity partyEntity) {
        ClaimEntity mainClaim = partyEntity.getPcsCase().getClaims().getFirst();
        return mainClaim.getClaimParties().stream()
                .filter(claimPartyEntity -> claimPartyEntity.getParty().getId().equals(partyEntity.getId()))
                .findFirst()
                .map(ClaimPartyEntity::getRole)
                .orElseThrow(() -> new PartyNotFoundException("Party not found on main claim"));
    }

    public PartyEntity getPartyEntityById(UUID partyId, long caseReference) {
        return partyRepository.findByIdAndPcsCaseCaseReference(partyId, caseReference)
            .orElseThrow(() -> new IllegalStateException(
                "No party found for party ID: " + partyId + " and case reference: " + caseReference
            ));
    }

    /**
     * The claimant party a case is created with, so CaseAccessGroups derive during the draft phase.
     * Both organisation values are required: without them the case derives no group, and once CREATOR
     * is revoked at submit nobody can open it.
     */
    public void initialiseClaimant(PcsCaseEntity pcsCaseEntity, String organisationId,
                                   String organisationProfileId) {
        requireNonNullElseGet(organisationId, () -> {
            throw new IllegalArgumentException("Organisation must be provided to create a case");
        });
        requireNonNullElseGet(organisationProfileId, () -> {
            throw new IllegalArgumentException("Organisation profile ID must be provided to create a case");
        });
        PartyEntity claimantParty = new PartyEntity();
        claimantParty.setOrganisationId(organisationId);
        claimantParty.setOrganisationProfileId(organisationProfileId);
        pcsCaseEntity.addParty(claimantParty);
    }

    /** An organisation with no claim link is the party the case was created with, not a new one. */
    private Optional<PartyEntity> findInitialisedClaimant(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getParties().stream()
            .filter(party -> party.getOrganisationId() != null)
            .filter(party -> party.getClaimParties().isEmpty())
            .findFirst();
    }

    private PartyEntity populateClaimant(PartyEntity claimantParty, PCSCase pcsCase,
                                  String organisationIdForCurrentUser, String orgProfileId) {

        ClaimantInformation claimantInformation = pcsCase.getClaimantInformation();
        requireNonNull(claimantInformation, "Claimant must be provided");

        setClaimantOrgName(claimantInformation, claimantParty);
        setClaimantOrganisation(claimantParty, organisationIdForCurrentUser, orgProfileId);

        ClaimantContactPreferences claimantContactPreferences = pcsCase.getClaimantContactPreferences();
        AddressUK contactAddress = resolveContactAddress(claimantContactPreferences);

        claimantParty.setAddress(mapAddress(contactAddress));

        String contactEmail = isNotBlank(claimantContactPreferences.getOverriddenClaimantContactEmail())
            ? claimantContactPreferences.getOverriddenClaimantContactEmail()
            : claimantContactPreferences.getClaimantContactEmail();

        claimantParty.setEmailAddress(contactEmail);

        VerticalYesNo phoneNumberProvided = claimantContactPreferences.getClaimantProvidePhoneNumber();

        claimantParty.setPhoneNumberProvided(phoneNumberProvided);
        if (phoneNumberProvided == VerticalYesNo.YES) {
            claimantParty.setPhoneNumber(claimantContactPreferences.getClaimantContactPhoneNumber());
        }

        return partyRepository.save(claimantParty);
    }

    private void setClaimantOrganisation(PartyEntity claimantParty, String organisationId,
                                         String orgProfileId) {
        if (nonNull(organisationId)) {
            claimantParty.setOrganisationId(organisationId);
        }
        if (nonNull(orgProfileId)) {
            claimantParty.setOrganisationProfileId(orgProfileId);
        }
    }

    private static void setClaimantOrgName(ClaimantInformation claimantInformation, PartyEntity claimantParty) {
        if (claimantInformation.getOrgNameFound() == YesOrNo.NO) {
            claimantParty.setNameOverridden(YesOrNo.YES);
            claimantParty.setOrgName(claimantInformation.getFallbackClaimantName());
        } else if (claimantInformation.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
            claimantParty.setNameOverridden(YesOrNo.YES);
            claimantParty.setOrgName(claimantInformation.getOverriddenClaimantName());
        } else {
            claimantParty.setNameOverridden(YesOrNo.NO);
            claimantParty.setOrgName(claimantInformation.getClaimantName());
        }
    }

    private List<PartyEntity> createDefendants(PCSCase pcsCase) {
        requireNonNull(pcsCase.getDefendant1(), "Defendant 1 must be provided");

        List<PartyEntity> allDefendants = new ArrayList<>();
        allDefendants.add(createDefendant(pcsCase.getDefendant1()));

        if (pcsCase.getAddAnotherDefendant() == VerticalYesNo.YES) {
            pcsCase.getAdditionalDefendants().stream()
                .map(ListValue::getValue)
                .map(this::createDefendant)
                .forEach(allDefendants::add);
        }

        partyRepository.saveAll(allDefendants);

        return allDefendants;
    }

    private PartyEntity createDefendant(DefendantDetails defendantDetails) {
        PartyEntity defendantEntity = new PartyEntity();

        VerticalYesNo nameKnown = defendantDetails.getNameKnown();
        defendantEntity.setNameKnown(nameKnown);
        if (nameKnown == VerticalYesNo.YES) {
            defendantEntity.setFirstName(defendantDetails.getFirstName());
            defendantEntity.setLastName(defendantDetails.getLastName());
        }

        VerticalYesNo addressKnown = defendantDetails.getAddressKnown();
        defendantEntity.setAddressKnown(addressKnown);
        if (addressKnown == VerticalYesNo.YES) {
            VerticalYesNo addressSameAsPossession = defendantDetails.getAddressSameAsPossession();
            defendantEntity.setAddressSameAsProperty(addressSameAsPossession);
            if (addressSameAsPossession == VerticalYesNo.NO) {
                defendantEntity.setAddress(mapAddress(defendantDetails.getCorrespondenceAddress()));
            }
        }

        return defendantEntity;
    }

    private List<PartyEntity> createUnderlesseeOrMortgagees(PCSCase pcsCase) {
        if (pcsCase.getHasUnderlesseeOrMortgagee() != VerticalYesNo.YES) {
            return List.of();
        }

        requireNonNull(pcsCase.getUnderlesseeOrMortgagee1(), "Underlessee or mortgagee 1 must be provided");

        List<PartyEntity> allUnderlesseeOrMortgagees = new ArrayList<>();
        allUnderlesseeOrMortgagees.add(createUnderlesseeOrMortgagee(pcsCase.getUnderlesseeOrMortgagee1()));

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES) {
            pcsCase.getAdditionalUnderlesseeOrMortgagee().stream()
                .map(ListValue::getValue)
                .map(this::createUnderlesseeOrMortgagee)
                .forEach(allUnderlesseeOrMortgagees::add);
        }

        partyRepository.saveAll(allUnderlesseeOrMortgagees);

        return allUnderlesseeOrMortgagees;
    }

    private PartyEntity createUnderlesseeOrMortgagee(UnderlesseeMortgageeDetails underlesseeMortgageeDetails) {

        PartyEntity underlesseeMortgageeEntity = new PartyEntity();

        VerticalYesNo nameKnown = underlesseeMortgageeDetails.getNameKnown();
        underlesseeMortgageeEntity.setNameKnown(nameKnown);
        if (nameKnown == VerticalYesNo.YES) {
            underlesseeMortgageeEntity.setOrgName(underlesseeMortgageeDetails.getName());
        }

        VerticalYesNo addressKnown = underlesseeMortgageeDetails.getAddressKnown();
        underlesseeMortgageeEntity.setAddressKnown(addressKnown);
        if (addressKnown == VerticalYesNo.YES) {
            underlesseeMortgageeEntity
                .setAddress(mapAddress(underlesseeMortgageeDetails.getAddress()));
        }

        return underlesseeMortgageeEntity;
    }

    private AddressEntity mapAddress(AddressUK address) {
        return address != null
            ? addressMapper.toAddressEntityAndNormalise(address) : null;
    }

    private AddressUK resolveContactAddress(ClaimantContactPreferences contactPreferences) {
        if (contactPreferences.getOverriddenClaimantContactAddress() != null) {
            return contactPreferences.getOverriddenClaimantContactAddress();
        }
        return contactPreferences.getOrganisationAddress();
    }

    private static ClaimPartyEntity getClaimParty(ClaimEntity claim, UUID partyId) {
        return claim.getClaimParties().stream()
            .filter(claimPartyEntity -> partyId.equals(claimPartyEntity.getParty().getId()))
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException("Party not found"));
    }

}
