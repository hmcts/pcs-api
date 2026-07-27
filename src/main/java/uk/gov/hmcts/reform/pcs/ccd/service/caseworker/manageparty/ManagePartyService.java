package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ManagePartyService {

    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public void addParty(AddPartyDetails addPartyDetails, PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity,
                          UUID actingForPartyId) {

        PartyType partyType = addPartyDetails.getAddPartyType();
        PartyEntity partyToAdd = buildParty(partyType, addPartyDetails);
        PartyEntity actingForParty = partyType == PartyType.LITIGATION_FRIEND
            ? resolveActingForParty(actingForPartyId)
            : null;

        pcsCaseEntity.addParty(partyToAdd);
        partyRepository.save(partyToAdd);

        claimEntity.addParty(partyToAdd, PartyRole.valueOf(partyType.name()), actingForParty);
        claimRepository.save(claimEntity);
    }

    private PartyEntity buildParty(PartyType partyType, AddPartyDetails addPartyDetails) {
        PartyEntity partyEntity = new PartyEntity();
        switch (partyType) {
            case CLAIMANT -> addClaimant(addPartyDetails, partyEntity);
            case DEFENDANT -> addDefendant(addPartyDetails, partyEntity);
            case LITIGATION_FRIEND -> addLitigationFriend(addPartyDetails, partyEntity);
        }
        return partyEntity;
    }

    private PartyEntity resolveActingForParty(UUID actingForPartyId) {
        return actingForPartyId != null
            ? partyRepository.findById(actingForPartyId)
                .orElseThrow(() -> new PartyNotFoundException("Party not found"))
            : null;
    }

    public void addClaimant(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setOrgName(addPartyDetails.getClaimantOrganisationName() != null
            ? addPartyDetails.getClaimantOrganisationName() : addPartyDetails.getClaimantName());
        partyEntity.setNameKnown(VerticalYesNo.YES);

        applyContactDetails(partyEntity, addPartyDetails.getClaimantAddress(),
            addPartyDetails.getClaimantEmail(), addPartyDetails.getClaimantPhoneNumber());
    }

    public void addDefendant(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setFirstName(addPartyDetails.getFirstName());
        partyEntity.setLastName(addPartyDetails.getLastName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        partyEntity.setDateOfBirth(addPartyDetails.getDefendantDateOfBirth());

        applyContactDetails(partyEntity, addPartyDetails.getDefendantAddress(),
            addPartyDetails.getDefendantEmail(), addPartyDetails.getDefendantPhoneNumber());
    }

    public void addLitigationFriend(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setOrgName(addPartyDetails.getLitigationFriendOrganisationName() != null
            ? addPartyDetails.getLitigationFriendOrganisationName() : addPartyDetails.getLitigationFriendName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        partyEntity.setDateOfBirth(addPartyDetails.getLitigationFriendDateOfBirth());

        applyContactDetails(partyEntity, addPartyDetails.getLitigationFriendAddress(),
            addPartyDetails.getLitigationFriendEmail(), addPartyDetails.getLitigationFriendPhoneNumber());
    }

    private void applyContactDetails(PartyEntity partyEntity, AddressUK address, String email, String phoneNumber) {
        partyEntity.setAddressKnown(VerticalYesNo.from(address != null));
        partyEntity.setAddress(address != null ? addressMapper.toAddressEntityAndNormalise(address) : null);
        partyEntity.setEmailAddress(email);
        partyEntity.setPhoneNumberProvided(VerticalYesNo.from(phoneNumber != null));
        partyEntity.setPhoneNumber(phoneNumber);
    }
}
