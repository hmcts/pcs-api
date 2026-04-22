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
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing defendant contact preferences.
 * Handles saving contact preferences and updating party contact details.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimResponseService {

    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

    /**
     * Saves defendant's contact preferences and contact details.
     * Finds the defendant party by the current user's IDAM ID and case reference updates their information.
     *
     * @throws IllegalStateException if no party found for the current user's IDAM ID
     */
    public void saveDraftData(PossessionClaimResponse dataFromDraftTable, long caseReference) {
        UUID currentUserIdamId = securityContextService.getCurrentUserId();

        if (currentUserIdamId == null) {
            log.error("Cannot save contact preferences: current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        PartyEntity defendant = partyService.getPartyEntityByIdamId(currentUserIdamId, caseReference);

        //save to relevant tables
        boolean shouldSavePhoneNumAndTextPreference =
            saveContactPreferences(defendant, dataFromDraftTable.getDefendantResponses());
        updatePartyContactDetails(defendant, dataFromDraftTable.getDefendantContactDetails(),
            shouldSavePhoneNumAndTextPreference);

        // Copy dateOfBirth from defendantResponses to party entity if present
        if (dataFromDraftTable.getDefendantResponses() != null
            && dataFromDraftTable.getDefendantResponses().getDateOfBirth() != null) {
            defendant.setDateOfBirth(dataFromDraftTable.getDefendantResponses().getDateOfBirth());
            log.debug("Updated date of birth from defendantResponses for party ID: {}", defendant.getId());
        }

        log.debug("Successfully saved contact preferences for defendant with IDAM ID: {}", currentUserIdamId);
    }

    /**
     * Updates party's contact details (phone number, email address, first name, and last name).
     * Only updates if the values are provided (non-blank).
     */
    private void updatePartyContactDetails(PartyEntity party, DefendantContactDetails defendantResponse,
                                              boolean shouldSavePhoneNumAndTextPreference) {

        if (StringUtils.isNotBlank(defendantResponse.getParty().getFirstName())) {
            party.setFirstName(defendantResponse.getParty().getFirstName());
            log.debug("Updated first name for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantResponse.getParty().getLastName())) {
            party.setLastName(defendantResponse.getParty().getLastName());
            log.debug("Updated last name for party ID: {}", party.getId());
        }

        if (defendantResponse.getParty().getDateOfBirth() != null) {
            party.setDateOfBirth(defendantResponse.getParty().getDateOfBirth());
            log.debug("Updated date of birth for party ID: {}", party.getId());
        }

        if (shouldSavePhoneNumAndTextPreference
            && StringUtils.isNotBlank(defendantResponse.getParty().getPhoneNumber())) {
            party.setPhoneNumber(defendantResponse.getParty().getPhoneNumber());
            log.debug("Updated phone number for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantResponse.getParty().getEmailAddress())) {
            party.setEmailAddress(defendantResponse.getParty().getEmailAddress());
            log.debug("Updated email address for party ID: {}", party.getId());
        }

        AddressUK newAddress = defendantResponse.getParty().getAddress();

        if (newAddress != null && StringUtils.isNotBlank(newAddress.getAddressLine1())) {
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
        }
    }

    /**
     * Creates and saves contact preferences entity with null-safe conversion.
     * Defaults null preferences to false (no contact).
     */
    private boolean saveContactPreferences(PartyEntity party, DefendantResponses defendantResponse) {
        ContactPreferencesEntity contactPrefs = party.getContactPreferences();

        if (contactPrefs == null) {
            contactPrefs = new ContactPreferencesEntity();
            party.setContactPreferences(contactPrefs);
        }

        contactPrefs.setPreferenceType(defendantResponse.getPreferenceType());
        contactPrefs.setContactByPhone(defendantResponse.getContactByPhone());

        boolean shouldSavePhoneNumAndTextPreference = Optional.ofNullable(defendantResponse.getContactByPhone())
            .map(VerticalYesNo::toBoolean)
            .orElse(false);
        if (shouldSavePhoneNumAndTextPreference) {
            contactPrefs.setContactByText(defendantResponse.getContactByText());
        }

        log.debug("Saved contact preferences for party ID: {}", party.getId());
        return shouldSavePhoneNumAndTextPreference;
    }
}
