package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.ClaimantOrgNameListCreator;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

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
    private final ClaimantOrgNameListCreator claimantOrgNameListCreator;

    /**
     * Maps view-populated PCSCase and matched defendant to PossessionClaimResponse.
     *
     * @param pcsCase View-populated case with allClaimants (PartyRole.CLAIMANT only)
     * @param matchedDefendant The matched defendant entity from DefendantAccessValidator
     * @return PossessionClaimResponse with claimant orgs, contact details, and responses
     */
    public PossessionClaimResponse mapFrom(PCSCase pcsCase, PartyEntity matchedDefendant) {
        DefendantContactDetails contactDetails = buildContactDetails(pcsCase, matchedDefendant);
        String currentDefendantPartyId = matchedDefendant.getId() != null ? matchedDefendant.getId().toString() : null;

        Party claimantEnteredDetails = buildPartyFromEntity(matchedDefendant, pcsCase);

        // Extract org names from CLAIMANT-role parties (pre-filtered by PCSCaseView.getPartyMap)
        List<ListValue<String>> claimantOrgs = claimantOrgNameListCreator.createClaimantOrgNameList(pcsCase);

        return PossessionClaimResponse.builder()
            .claimantOrganisations(claimantOrgs)
            .defendantContactDetails(contactDetails)
            .claimantEnteredDefendantDetails(claimantEnteredDetails)
            .currentDefendantPartyId(currentDefendantPartyId)
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
        AddressUK contactAddress = resolveAddress(matchedDefendant, pcsCase);

        Party defendantParty = Party.builder()
            .firstName(matchedDefendant.getFirstName())
            .lastName(matchedDefendant.getLastName())
            .emailAddress(matchedDefendant.getEmailAddress())
            .address(contactAddress)
            .phoneNumber(matchedDefendant.getPhoneNumber())
            .phoneNumberProvided(matchedDefendant.getPhoneNumberProvided())
            .dateOfBirth(matchedDefendant.getDateOfBirth())
            .build();

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

}
