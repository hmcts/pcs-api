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

import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AddPartyService {

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

        claimEntity.addParty(partyToAdd, toPartyRole(partyType), actingForParty);
        claimRepository.save(claimEntity);
    }

    private PartyRole toPartyRole(PartyType partyType) {
        return switch (partyType) {
            case CLAIMANT -> PartyRole.CLAIMANT;
            case DEFENDANT -> PartyRole.DEFENDANT;
            case LITIGATION_FRIEND -> PartyRole.LITIGATION_FRIEND;
        };
    }

    private PartyEntity buildParty(PartyType partyType, AddPartyDetails addPartyDetails) {
        PartyEntity partyEntity = new PartyEntity();
        switch (partyType) {
            case CLAIMANT -> applyClaimantDetails(addPartyDetails, partyEntity);
            case DEFENDANT -> applyDefendantDetails(addPartyDetails, partyEntity);
            case LITIGATION_FRIEND -> applyLitigationFriendDetails(addPartyDetails, partyEntity);
        }
        return partyEntity;
    }

    private PartyEntity resolveActingForParty(UUID actingForPartyId) {
        Objects.requireNonNull(actingForPartyId, "Acting for party is required for litigation friend");
        return partyRepository.findById(actingForPartyId)
            .orElseThrow(() -> new PartyNotFoundException("Acting for party not found"));
    }

    private void applyClaimantDetails(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setFirstName(addPartyDetails.getClaimantFirstName());
        partyEntity.setLastName(addPartyDetails.getClaimantLastName());
        partyEntity.setOrgName(addPartyDetails.getClaimantOrganisationName());
        partyEntity.setNameKnown(VerticalYesNo.YES);

        applyContactDetails(partyEntity, addPartyDetails.getClaimantAddress(),
            addPartyDetails.getClaimantEmail(), addPartyDetails.getClaimantPhoneNumber());
    }

    private void applyDefendantDetails(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setFirstName(addPartyDetails.getFirstName());
        partyEntity.setLastName(addPartyDetails.getLastName());
        partyEntity.setNameKnown(VerticalYesNo.YES);
        partyEntity.setDateOfBirth(addPartyDetails.getDefendantDateOfBirth());

        applyContactDetails(partyEntity, addPartyDetails.getDefendantAddress(),
            addPartyDetails.getDefendantEmail(), addPartyDetails.getDefendantPhoneNumber());
    }

    private void applyLitigationFriendDetails(AddPartyDetails addPartyDetails, PartyEntity partyEntity) {
        partyEntity.setFirstName(addPartyDetails.getLitigationFriendFirstName());
        partyEntity.setLastName(addPartyDetails.getLitigationFriendLastName());
        partyEntity.setOrgName(addPartyDetails.getLitigationFriendOrganisationName());
        partyEntity.setNameKnown(VerticalYesNo.YES);

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
