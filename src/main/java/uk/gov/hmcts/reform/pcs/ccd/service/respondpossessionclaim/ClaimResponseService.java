package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
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
        saveContactPreferences(defendant, dataFromDraftTable.getDefendantResponses());
        updatePartyContactDetails(defendant, dataFromDraftTable.getDefendantContactDetails(),
                                  dataFromDraftTable.getDefendantResponses());

        // Copy dateOfBirth from defendantResponses to party entity if present
        if (dataFromDraftTable.getDefendantResponses() != null
            && dataFromDraftTable.getDefendantResponses().getDateOfBirth() != null) {
            defendant.setDateOfBirth(dataFromDraftTable.getDefendantResponses().getDateOfBirth());
            log.debug("Updated date of birth from defendantResponses for party ID: {}", defendant.getId());
        }

        log.debug("Successfully saved contact preferences for defendant with IDAM ID: {}", currentUserIdamId);
    }

    /**
     * Updates party's details (phone number, email address, date of birth ).
     * Only updates if the values are provided (non-blank).
     */
    private void updatePartyContactDetails(PartyEntity party, DefendantContactDetails defendantContactDetails,
                                           DefendantResponses defendantResponses) {
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
