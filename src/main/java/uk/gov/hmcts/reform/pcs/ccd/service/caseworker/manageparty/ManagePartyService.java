package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ManagePartyService {

    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final AddressMapper addressMapper;

    public void addParty(AddPartyDetails addPartyDetails, PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity,
                          UUID actingForPartyId) {

        PartyEntity partyEntity = new PartyEntity();
        switch (addPartyDetails.getAddPartyType()) {
            case CLAIMANT -> addClaimant(addPartyDetails, partyEntity);
            case DEFENDANT -> addDefendant(addPartyDetails, partyEntity);
            case LITIGATION_FRIEND -> addLitigationFriend(addPartyDetails, partyEntity);
        }
        pcsCaseEntity.addParty(partyEntity);
        partyRepository.save(partyEntity);

        PartyRole role = PartyRole.valueOf(addPartyDetails.getAddPartyType().name());
        PartyEntity actingForParty = actingForPartyId != null
            ? partyRepository.findById(actingForPartyId).orElse(null) : null;
        claimEntity.addParty(partyEntity, role, actingForParty);
        claimRepository.save(claimEntity);
    }

    public void addClaimant(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setOrgName(StringUtils.isNotBlank(addPartyDetails.getClaimantOrganisationName())
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
        partyEntity.setOrgName(StringUtils.isNotBlank(addPartyDetails.getLitigationFriendOrganisationName())
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
