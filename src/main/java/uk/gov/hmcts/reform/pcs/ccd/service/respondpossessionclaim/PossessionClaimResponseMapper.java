package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;

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
        DefendantResponses responses = DefendantResponses.builder().build();

        // Extract org names from CLAIMANT-role parties (pre-filtered by PCSCaseView.getPartyMap)
        List<ListValue<String>> claimantOrgs = extractClaimantOrganisations(pcsCase);

        return PossessionClaimResponse.builder()
            .claimantOrganisations(claimantOrgs)
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses)
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
            .nameKnown(matchedDefendant.getNameKnown())
            .emailAddress(matchedDefendant.getEmailAddress())
            .address(contactAddress)
            .addressKnown(matchedDefendant.getAddressKnown())
            .addressSameAsProperty(matchedDefendant.getAddressSameAsProperty())
            .phoneNumber(matchedDefendant.getPhoneNumber())
            .phoneNumberProvided(matchedDefendant.getPhoneNumberProvided())
            .build();

        return DefendantContactDetails.builder()
            .party(defendantParty)
            .build();
    }

    /**
     * Resolves defendant's contact address.
     * If defendant's address is same as property, use property address from PCSCase (already AddressUK).
     * Otherwise convert defendant's address entity to AddressUK.
     */
    private AddressUK resolveAddress(PartyEntity defendantEntity, PCSCase pcsCase) {
        if (defendantEntity.getAddressSameAsProperty() != null
            && defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return pcsCase.getPropertyAddress();  // Already AddressUK from view
        } else {
            return addressMapper.toAddressUK(defendantEntity.getAddress());
        }
    }

    /*
     * Extracts organisation names from claimant parties.
     * allClaimants is pre-filtered to PartyRole.CLAIMANT by PCSCaseView.
     * Supports multiple claimants and filters out null/empty org names.
     * Returns ListValue-wrapped strings for CCD collection compatibility.
     */
    private List<ListValue<String>> extractClaimantOrganisations(PCSCase pcsCase) {
        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();

        if (allClaimants == null || allClaimants.isEmpty()) {
            log.warn("No claimant parties found in case, returning empty organisation list");
            return List.of();
        }

        List<String> orgNames = allClaimants.stream()
            .map(ListValue::getValue)
            .map(Party::getOrgName)
            .filter(StringUtils::isNotBlank)
            .toList();

        return java.util.stream.IntStream.range(0, orgNames.size())
            .mapToObj(i -> ListValue.<String>builder()
                .id("claimant-org-" + (i + 1))
                .value(orgNames.get(i))
                .build())
            .toList();
    }
}
