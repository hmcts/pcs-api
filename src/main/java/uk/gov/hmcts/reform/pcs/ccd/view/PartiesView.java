package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class PartiesView {

    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

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
        Party party = shouldRedact ? toPartialParty(partyEntity) : toParty(partyEntity);

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
            .build();
    }

    private Party toPartialParty(PartyEntity entity) {
        return Party.builder()
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .orgName(entity.getOrgName())
            .build();
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return modelMapper.map(address, AddressUK.class);
    }
}
