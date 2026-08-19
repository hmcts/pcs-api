package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.Optional;

/**
 * Service for managing defendant contact preferences.
 * Handles saving contact preferences and updating party contact details.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimResponseService {

    private final ModelMapper modelMapper;

    /**
     * Saves defendant's contact preferences and contact details for the given defendant party.
     *
     * @throws IllegalStateException if no party is found
     */
    public void saveDraftDataForParty(PossessionClaimResponse dataFromDraftTable, PartyEntity defendantParty) {

        if (defendantParty == null) {
            throw new IllegalStateException("defendant party is null");
        }

        saveContactPreferences(defendantParty, dataFromDraftTable.getDefendantResponses());
        updatePartyContactDetails(defendantParty, dataFromDraftTable.getDefendantContactDetails(), dataFromDraftTable
            .getDefendantResponses());

        if (dataFromDraftTable.getDefendantResponses() != null
            && dataFromDraftTable.getDefendantResponses().getDateOfBirth() != null) {
            defendantParty.setDateOfBirth(dataFromDraftTable.getDefendantResponses().getDateOfBirth());
            log.debug("Updated date of birth from defendantResponses for party ID: {}", defendantParty.getId());
        }
    }

    /**
     * Updates party's contact details (phone number, email address, first name, and last name).
     * Name and address are only updated if the claimant did not provide them, indicated by the
     * confirmation fields being null (the confirmation question is only shown when the claimant provided the value).
     * Only updates if the values are provided (non-blank).
     */
    private void updatePartyContactDetails(PartyEntity party, DefendantContactDetails defendantContactDetails,
                                           DefendantResponses defendantResponses) {
        boolean nameNotConfirmed = defendantResponses.getDefendantNameConfirmation() != VerticalYesNo.YES;

        if (nameNotConfirmed && StringUtils.isNotBlank(defendantContactDetails.getParty().getFirstName())) {
            party.setFirstName(defendantContactDetails.getParty().getFirstName());
            log.debug("Updated first name for party ID: {}", party.getId());
        }

        if (nameNotConfirmed && StringUtils.isNotBlank(defendantContactDetails.getParty().getLastName())) {
            party.setLastName(defendantContactDetails.getParty().getLastName());
            log.debug("Updated last name for party ID: {}", party.getId());
        }

        if (nameNotConfirmed
            && (StringUtils.isNotBlank(defendantContactDetails.getParty().getFirstName())
                || StringUtils.isNotBlank(defendantContactDetails.getParty().getLastName()))) {
            party.setNameKnown(VerticalYesNo.YES);
        }

        if (defendantContactDetails.getParty().getDateOfBirth() != null) {
            party.setDateOfBirth(defendantContactDetails.getParty().getDateOfBirth());
            log.debug("Updated date of birth for party ID: {}", party.getId());
        }

        if (isContactByPhoneSelected(defendantResponses.getContactByPhone())
            && StringUtils.isNotBlank(defendantContactDetails.getParty().getPhoneNumber())) {
            party.setPhoneNumber(defendantContactDetails.getParty().getPhoneNumber());
            log.debug("Updated phone number for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantContactDetails.getParty().getEmailAddress())) {
            party.setEmailAddress(defendantContactDetails.getParty().getEmailAddress());
            log.debug("Updated email address for party ID: {}", party.getId());
        }

        AddressUK newAddress = defendantContactDetails.getParty().getAddress();
        boolean isFallbackScenario = defendantResponses.getPropertyAddressConfirmation() != null;
        boolean disputedCorrespondenceAddress =
            defendantResponses.getCorrespondenceAddressConfirmation() == VerticalYesNo.NO;
        boolean hasNewAddress = newAddress != null && StringUtils.isNotBlank(newAddress.getAddressLine1());

        if ((isFallbackScenario || disputedCorrespondenceAddress) && hasNewAddress) {
            AddressEntity existingAddress = party.getAddress();

            if (existingAddress != null) {
                existingAddress.setAddressLine1(newAddress.getAddressLine1());
                existingAddress.setAddressLine2(newAddress.getAddressLine2());
                existingAddress.setAddressLine3(newAddress.getAddressLine3());
                existingAddress.setPostTown(newAddress.getPostTown());
                existingAddress.setCounty(newAddress.getCounty());
                existingAddress.setPostcode(newAddress.getPostCode());
                existingAddress.setCountry(newAddress.getCountry());
            } else {
                party.setAddress(modelMapper.map(newAddress, AddressEntity.class));
            }
            party.setAddressKnown(VerticalYesNo.YES);
        }

        if (disputedCorrespondenceAddress) {
            party.setAddressSameAsProperty(VerticalYesNo.NO);
        }
    }

    /**
     * Creates and saves contact preferences entity with null-safe conversion.
     * Defaults null preferences to false (no contact).
     */
    private void saveContactPreferences(PartyEntity party, DefendantResponses defendantResponse) {
        ContactPreferencesEntity contactPrefs = party.getContactPreferences();

        if (contactPrefs == null) {
            contactPrefs = new ContactPreferencesEntity();
            party.setContactPreferences(contactPrefs);
        }

        contactPrefs.setContactByEmail(defendantResponse.getContactByEmail());
        contactPrefs.setContactByPost(defendantResponse.getContactByPost());
        contactPrefs.setContactByPhone(defendantResponse.getContactByPhone());

        if (isContactByPhoneSelected(defendantResponse.getContactByPhone())) {
            contactPrefs.setContactByText(defendantResponse.getContactByText());
        }

        log.debug("Saved contact preferences for party ID: {}", party.getId());
    }

    private boolean isContactByPhoneSelected(VerticalYesNo contactByPhone) {
        return Optional.ofNullable(contactByPhone)
            .map(VerticalYesNo::toBoolean)
            .orElse(false);
    }
}
