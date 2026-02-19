package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

/**
 * Service for managing defendant contact preferences.
 * Handles saving contact preferences and updating party contact details.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PossessionClaimResponsePersistenceService {

    private final PartyRepository partyRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

    /**
     * Saves defendant's contact preferences and contact details.
     * Finds the defendant party by the current user's IDAM ID and case reference updates their information.
     *
     * @throws IllegalStateException if no party found for the current user's IDAM ID
     */
    public void saveDraftData(PossessionClaimResponse dataFromDraftTable) {
        UUID currentUserIdamId = securityContextService.getCurrentUserId();

        if (currentUserIdamId == null) {
            log.error("Cannot save contact preferences: current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        PartyEntity defendant = findDefendantByIdamId(currentUserIdamId);

        //save to relevant tables
        updatePartyContactDetails(defendant, dataFromDraftTable.getDefendantContactDetails());
        saveContactPreferences(defendant, dataFromDraftTable.getDefendantResponses());

        log.debug("Successfully saved contact preferences for defendant with IDAM ID: {}", currentUserIdamId);
    }

    /**
     * Finds a defendant party by their IDAM ID.
     * Similar pattern to PcsCaseMergeService.setPcqIdForCurrentUser()
     */
    private PartyEntity findDefendantByIdamId(UUID idamId) {
        return partyRepository.findByIdamId(idamId)
            .orElseThrow(() -> new IllegalStateException(
                "No party found for IDAM ID: " + idamId));
    }

    /**
     * Updates party's contact details (phone number and email address).
     * Only updates if the values are provided (non-blank).
     */
    private void updatePartyContactDetails(PartyEntity party, DefendantContactDetails defendantResponse) {
        boolean updated = false;

        if (StringUtils.isNotBlank(defendantResponse.getParty().getPhoneNumber())) {
            party.setPhoneNumber(defendantResponse.getParty().getPhoneNumber());
            updated = true;
            log.debug("Updated phone number for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantResponse.getParty().getEmailAddress())) {
            party.setEmailAddress(defendantResponse.getParty().getEmailAddress());
            updated = true;
            log.debug("Updated email address for party ID: {}", party.getId());
        }

        AddressUK newAddress = defendantResponse.getParty().getAddress();

        if (newAddress != null && StringUtils.isNotBlank(newAddress.getAddressLine1())) {
            AddressEntity existingAddress = party.getAddress();

            if (existingAddress != null) {
                existingAddress.setAddressLine1(defendantResponse.getParty().getAddress().getAddressLine1());
                existingAddress.setAddressLine2(newAddress.getAddressLine2());
                existingAddress.setAddressLine3(newAddress.getAddressLine3());
                existingAddress.setPostTown(newAddress.getPostTown());
                existingAddress.setCounty(newAddress.getCounty());
                existingAddress.setPostcode(newAddress.getPostCode());
                existingAddress.setCountry(newAddress.getCountry());
            } else {
                party.setAddress(modelMapper.map(newAddress, AddressEntity.class));
            }
            updated = true;
        }

        if (updated) {
            partyRepository.save(party);
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

        contactPrefs.setContactByEmail(YesOrNoConverter.toBoolean(defendantResponse.getContactByEmail()));
        contactPrefs.setContactByText(YesOrNoConverter.toBoolean(defendantResponse.getContactByText()));
        contactPrefs.setContactByPost(YesOrNoConverter.toBoolean(defendantResponse.getContactByPost()));
        contactPrefs.setContactByPhone(YesOrNoConverter.toBoolean(defendantResponse.getContactByPhone()));

        partyRepository.save(party);
        log.debug("Saved contact preferences for party ID: {}", party.getId());
    }
}
