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

@Service
@AllArgsConstructor
public class ManagePartyService {

    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final AddressMapper addressMapper;

    public void addParty(AddPartyDetails addPartyDetails, PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {

        PartyEntity partyEntity = switch (addPartyDetails.getAddPartyType()) {
            case CLAIMANT -> addClaimant(addPartyDetails);
            case DEFENDANT -> addDefendant(addPartyDetails);
            case LITIGATION_FRIEND -> addLitigationFriend(addPartyDetails);
        };
        pcsCaseEntity.addParty(partyEntity);
        partyRepository.save(partyEntity);

        PartyRole role = PartyRole.valueOf(addPartyDetails.getAddPartyType().name());
        claimEntity.addParty(partyEntity, role);
        claimRepository.save(claimEntity);
    }

    public PartyEntity addClaimant(AddPartyDetails addPartyDetails) {
        PartyEntity partyEntity = new PartyEntity();
        if (StringUtils.isNotBlank(addPartyDetails.getClaimantOrganisationName())) {
            partyEntity.setOrgName(addPartyDetails.getClaimantOrganisationName());
        }
        applyFullName(partyEntity, addPartyDetails.getClaimantName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        applyContactDetails(partyEntity, addPartyDetails.getClaimantAddress(),
            addPartyDetails.getClaimantEmail(), addPartyDetails.getClaimantPhoneNumber());
        return partyEntity;
    }

    public PartyEntity addDefendant(AddPartyDetails addPartyDetails) {
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setFirstName(addPartyDetails.getFirstName());
        partyEntity.setLastName(addPartyDetails.getLastName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        partyEntity.setDateOfBirth(addPartyDetails.getDefendantDateOfBirth());
        applyContactDetails(partyEntity, addPartyDetails.getDefendantAddress(),
            addPartyDetails.getDefendantEmail(), addPartyDetails.getDefendantPhoneNumber());
        return partyEntity;
    }

    public PartyEntity addLitigationFriend(AddPartyDetails addPartyDetails) {
        PartyEntity partyEntity = new PartyEntity();
        if (StringUtils.isNotBlank(addPartyDetails.getLitigationFriendOrganisationName())) {
            partyEntity.setOrgName(addPartyDetails.getLitigationFriendOrganisationName());
        }
        applyFullName(partyEntity, addPartyDetails.getLitigationFriendName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        partyEntity.setDateOfBirth(addPartyDetails.getLitigationFriendDateOfBirth());
        applyContactDetails(partyEntity, addPartyDetails.getLitigationFriendAddress(),
            addPartyDetails.getLitigationFriendEmail(), addPartyDetails.getLitigationFriendPhoneNumber());

        // The party they're acting for (from PCSCase.partyRadioList) still needs linking
        return partyEntity;
    }

    private void applyFullName(PartyEntity partyEntity, String fullName) {
        if (StringUtils.isBlank(fullName)) {
            return;
        }
        String[] parts = fullName.trim().split(" ", 2);
        partyEntity.setFirstName(parts[0]);
        partyEntity.setLastName(parts.length > 1 ? parts[1] : null);
    }

    private void applyContactDetails(PartyEntity partyEntity, AddressUK address, String email, String phoneNumber) {
        partyEntity.setAddressKnown(VerticalYesNo.from(address != null));
        partyEntity.setAddress(address != null ? addressMapper.toAddressEntityAndNormalise(address) : null);
        partyEntity.setEmailAddress(email);
        partyEntity.setPhoneNumberProvided(VerticalYesNo.from(phoneNumber != null));
        partyEntity.setPhoneNumber(phoneNumber);
    }
}
