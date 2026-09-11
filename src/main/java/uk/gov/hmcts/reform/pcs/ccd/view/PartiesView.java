package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.LegalRepresentative;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.Organisation;
import uk.gov.hmcts.reform.pcs.ccd.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class PartiesView {

    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims.isEmpty()) {
            return;
        }

        boolean isCitizen = securityContextService.getCurrentUserDetails().getRoles()
            .contains(UserRole.CITIZEN.getRole());
        UUID currentUserId = securityContextService.getCurrentUserId();
        List<ClaimPartyEntity> claimParties = claims.getFirst().getClaimParties();

        pcsCase.setAllClaimants(mapPartiesByRole(claimParties, PartyRole.CLAIMANT, isCitizen, currentUserId));
        pcsCase.setAllDefendants(mapPartiesByRole(claimParties, PartyRole.DEFENDANT, isCitizen, currentUserId));
        pcsCase.setAllUnderlesseeOrMortgagees(mapPartiesByRole(claimParties, PartyRole.UNDERLESSEE_OR_MORTGAGEE,
                                                               isCitizen, currentUserId));
        pcsCase.setAllLitigationFriends(mapPartiesByRole(claimParties, PartyRole.LITIGATION_FRIEND,
                                                          isCitizen, currentUserId));
    }

    /**
     * The organisation policy that Notice of Change and the data store's group-access stamping key on.
     * The role is the NoC case role the challenge question answers with, and the organisation is the
     * defendant's active legal representative, or an organisation with null fields when unrepresented
     * so the node is still present for the data store to read.
     */
    private OrganisationPolicy<UserRole> buildOrganisationPolicy(PartyEntity partyEntity) {
        Organisation organisation = activeLegalRepOrganisation(partyEntity)
            .map(orgEntity -> Organisation.builder()
                .organisationId(orgEntity.getOrganisationId())
                .organisationName(orgEntity.getOrganisationName())
                .build())
            .orElseGet(Organisation::new);

        return OrganisationPolicy.<UserRole>builder()
            .organisation(organisation)
            .orgPolicyCaseAssignedRole(UserRole.DEFENDANT_SOLICITOR)
            .build();
    }

    private List<ListValue<Party>> mapPartiesByRole(List<ClaimPartyEntity> claimParties, PartyRole role,
                                                    boolean isCitizen, UUID currentUserId) {
        List<ListValue<Party>> result = claimParties.stream()
            .filter(cp -> cp.getRole() == role)
            .map(cp -> toListValue(cp, isCitizen, currentUserId))
            .toList();
        return result.isEmpty() ? null : result;
    }

    private ListValue<Party> toListValue(ClaimPartyEntity claimPartyEntity, boolean isCitizen, UUID currentUserId) {
        PartyEntity partyEntity = claimPartyEntity.getParty();
        boolean isCurrentUser = partyEntity.getIdamId() != null
            && partyEntity.getIdamId().equals(currentUserId);

        //Citizens only see full details for their own party, other party details are partial
        boolean shouldRedact = isCitizen && !isCurrentUser;
        Party party = shouldRedact
            ? toPartialParty(partyEntity)
            : toParty(partyEntity);

        //Only populated for litigation friends
        PartyEntity actingForParty = claimPartyEntity.getActingForParty();
        party.setActingForPartyId(actingForParty != null ? actingForParty.getId().toString() : null);

        if (claimPartyEntity.getRole() == PartyRole.DEFENDANT) {
            party.setOrganisationPolicy(buildOrganisationPolicy(partyEntity));
        }

        return ListValue.<Party>builder()
            .id(claimPartyEntity.getId().getPartyId().toString())
            .value(party)
            .build();
    }

    private Party toParty(PartyEntity entity) {
        return Party.builder()
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .orgName(entity.getOrgName())
            .nameKnown(entity.getNameKnown())
            .emailAddress(entity.getEmailAddress())
            .address(convertAddress(entity.getAddress()))
            .addressKnown(entity.getAddressKnown())
            .addressSameAsProperty(entity.getAddressSameAsProperty())
            .phoneNumber(entity.getPhoneNumber())
            .phoneNumberProvided(entity.getPhoneNumberProvided())
            .dateOfBirth(entity.getDateOfBirth())
            .legalRepresentative(buildLegalRepresentative(entity))
            .build();
    }

    private Party toPartialParty(PartyEntity entity) {
        return Party.builder()
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .orgName(entity.getOrgName())
            .nameKnown(entity.getNameKnown())
            .address(convertAddress(entity.getAddress()))
            .addressKnown(entity.getAddressKnown())
            .addressSameAsProperty(entity.getAddressSameAsProperty())
            .dateOfBirth(entity.getDateOfBirth())
            .build();
    }

    private LegalRepresentative buildLegalRepresentative(PartyEntity partyEntity) {
        return activeLegalRepOrganisation(partyEntity)
            .map(lro -> toLegalRepresentative(lro, partyEntity.getPcsCase().getCaseReference()))
            .orElse(null);
    }

    private Optional<OrganisationEntity> activeLegalRepOrganisation(PartyEntity partyEntity) {
        if (partyEntity == null || partyEntity.getClaimPartyOrganisationList() == null) {
            return Optional.empty();
        }

        return partyEntity.getClaimPartyOrganisationList().stream()
            .filter(legalRep -> legalRep != null && legalRep.getActive() == YesOrNo.YES)
            .map(ClaimPartyOrganisationEntity::getOrganisation)
            .filter(Objects::nonNull)
            .findFirst();
    }

    private LegalRepresentative toLegalRepresentative(OrganisationEntity orgEntity, Long caseRef) {
        // Look the row up directly: walking orgEntity.getClaimPartyContactDetails() loads every case the
        // organisation has ever been party to (one query per row), which is O(cases per organisation).
        Optional<ClaimPartyContactDetailsEntity> contactDetails = caseRef == null
            ? Optional.empty()
            : claimPartyContactDetailsRepository
                .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                    orgEntity.getOrganisationId(), caseRef);

        return LegalRepresentative.builder()
            .organisationName(orgEntity.getOrganisationName())
            .telephoneNumber(contactDetails.map(ClaimPartyContactDetailsEntity::getPhoneNumber)
                                 .orElse(null))
            .emailAddress(contactDetails.map(ClaimPartyContactDetailsEntity::getEmailAddress)
                              .orElse(null))
            .address(contactDetails.map(cd -> convertAddress(cd.getAddress()))
                         .orElse(null))
            .build();
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return modelMapper.map(address, AddressUK.class);
    }
}
