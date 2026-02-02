package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantData;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

/**
 * Maps view-populated PCSCase and matched defendant to PossessionClaimResponse.
 *
 * <p>This service initializes DefendantData with editable contact details from the matched defendant.
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
     * @param pcsCase View-populated case with claim data from view classes (TenancyLicenceView, RentDetailsView, etc.)
     * @param matchedDefendant The matched defendant entity from DefendantAccessValidator
     * @return PossessionClaimResponse with only defendantData section (claim data shown via PCSCase)
     */
    public PossessionClaimResponse mapFrom(PCSCase pcsCase, PartyEntity matchedDefendant) {
        DefendantData defendantData = buildDefendantData(pcsCase, matchedDefendant);

        return PossessionClaimResponse.builder()
            .defendantData(defendantData)
            .build();
    }

    /**
     * Builds DefendantData with editable contact details initialized from matched defendant.
     *
     * @param pcsCase View-populated case (used for property address resolution)
     * @param matchedDefendant The matched defendant entity
     * @return DefendantData with initialized contact details and empty responses
     */
    private DefendantData buildDefendantData(PCSCase pcsCase, PartyEntity matchedDefendant) {
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

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(defendantParty)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .build();

        return DefendantData.builder()
            .contactDetails(contactDetails)
            .responses(responses)
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
}
