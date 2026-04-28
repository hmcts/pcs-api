package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Maps view-populated PCSCase and matched defendant to PossessionClaimResponse.
 *
 * <p>This service initializes defendant contact details and responses from the matched defendant.
 * Claim data (tenancy, rent, notices) is shown via PCSCase fields with CitizenAccess annotation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PossessionClaimResponseMapper {

    private final AddressMapper addressMapper;

    /**
     * Maps view-populated PCSCase and matched defendant to PossessionClaimResponse.
     *
     * @param pcsCase View-populated case with allClaimants (PartyRole.CLAIMANT only)
     * @param matchedDefendant The matched defendant entity from DefendantAccessValidator
     * @return PossessionClaimResponse with claimant orgs, contact details, and responses
     */
    public PossessionClaimResponse mapFrom(PCSCase pcsCase, PartyEntity matchedDefendant) {
        DefendantContactDetails contactDetails = buildContactDetails(pcsCase, matchedDefendant);

        Party claimantEnteredDetails = buildPartyFromEntity(matchedDefendant, pcsCase);

        // Extract org names from CLAIMANT-role parties (pre-filtered by PCSCaseView.getPartyMap)
        List<ListValue<String>> claimantOrgs = extractClaimantOrganisations(pcsCase);

        List<ListValue<ClaimParty>> claimParties = buildClaimParties(pcsCase);

        return PossessionClaimResponse.builder()
            .claimantOrganisations(claimantOrgs)
            .defendantContactDetails(contactDetails)
            .claimantEnteredDefendantDetails(claimantEnteredDetails)
            .claimParties(claimParties)
            .build();
    }

    /**
     * Builds DefendantContactDetails with editable contact details initialized from matched defendant.
     *
     * @param pcsCase View-populated case (used for property address resolution)
     * @param matchedDefendant The matched defendant entity
     * @return DefendantContactDetails with initialized party details
     */
    private DefendantContactDetails buildContactDetails(PCSCase pcsCase, PartyEntity matchedDefendant) {
        Party defendantParty = buildPartyFromEntity(matchedDefendant, pcsCase);

        return DefendantContactDetails.builder()
            .party(defendantParty)
            .build();
    }


    public Party buildPartyFromEntity(PartyEntity partyEntity, PCSCase pcsCase) {
        AddressUK contactAddress = resolveAddress(partyEntity, pcsCase);

        return Party.builder()
            .firstName(partyEntity.getFirstName())
            .lastName(partyEntity.getLastName())
            .nameKnown(partyEntity.getNameKnown())
            .emailAddress(partyEntity.getEmailAddress())
            .address(contactAddress)
            .addressKnown(partyEntity.getAddressKnown())
            .addressSameAsProperty(partyEntity.getAddressSameAsProperty())
            .phoneNumber(partyEntity.getPhoneNumber())
            .phoneNumberProvided(partyEntity.getPhoneNumberProvided())
            .dateOfBirth(partyEntity.getDateOfBirth())
            .build();
    }

    /**
     * Resolves defendant's contact address.
     * If defendant's address is same as property, use property address from PCSCase (already AddressUK).
     * Otherwise convert defendant's address entity to AddressUK.
     */
    private AddressUK resolveAddress(PartyEntity defendantEntity, PCSCase pcsCase) {
        if (defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return pcsCase.getPropertyAddress();  // Already AddressUK from view
        } else {
            return Optional.ofNullable(defendantEntity.getAddress())
                .map(addressMapper::toAddressUK)
                .orElse(AddressUK.builder().build());
        }
    }

    /*
     * Builds a combined list of all named parties (claimants + defendants) with their roles.
     * Used for multi-party routing and display in the counterclaim journey.
     */
    public List<ListValue<ClaimParty>> buildClaimParties(PCSCase pcsCase) {
        List<ListValue<ClaimParty>> parties = new ArrayList<>();

        if (pcsCase.getAllClaimants() != null) {
            pcsCase.getAllClaimants().forEach(claimant -> parties.add(
                ListValue.<ClaimParty>builder()
                    .id(claimant.getId())
                    .value(toClaimParty(claimant.getValue(), PartyRole.CLAIMANT))
                    .build()
            ));
        }

        if (pcsCase.getAllDefendants() != null) {
            pcsCase.getAllDefendants().forEach(defendant -> parties.add(
                ListValue.<ClaimParty>builder()
                    .id(defendant.getId())
                    .value(toClaimParty(defendant.getValue(), PartyRole.DEFENDANT))
                    .build()
            ));
        }

        return parties;
    }

    private ClaimParty toClaimParty(Party party, PartyRole role) {
        return ClaimParty.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .role(role.name())
            .build();
    }

    /*
     * Extracts organisation names from claimant parties.
     * allClaimants is pre-filtered to PartyRole.CLAIMANT by PCSCaseView.
     * Supports multiple claimants, preserving original party IDs.
     * Returns ListValue-wrapped strings for CCD collection compatibility.
     */
    private List<ListValue<String>> extractClaimantOrganisations(PCSCase pcsCase) {
        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();

        if (allClaimants == null || allClaimants.isEmpty()) {
            log.warn("No claimant parties found in case, returning empty organisation list");
            return List.of();
        }

        return allClaimants.stream()
            .map(claimant -> ListValue.<String>builder()
                .id(claimant.getId())
                .value(claimant.getValue().getOrgName())
                .build())
            .toList();
    }
}
