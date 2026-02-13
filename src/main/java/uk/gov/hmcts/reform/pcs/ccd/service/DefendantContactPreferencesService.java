package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ContactPreferencesRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
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
public class DefendantContactPreferencesService {

    private final PartyRepository partyRepository;
    private final ContactPreferencesRepository contactPreferencesRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

    /**
     * Saves defendant's contact preferences and contact details.
     * Finds the defendant party by the current user's IDAM ID and updates their information.
     *
     * @param defendantResponse The defendant's response containing contact preferences and details
     * @throws IllegalStateException if no party found for the current user's IDAM ID
     */
    public void saveContactPreferences(PossessionClaimResponse defendantResponse) {
        UUID currentUserIdamId = securityContextService.getCurrentUserId();

        if (currentUserIdamId == null) {
            log.error("Cannot save contact preferences: current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        PartyEntity defendant = findDefendantByIdamId(currentUserIdamId);

        updatePartyContactDetails(defendant, defendantResponse);
        createAndSaveContactPreferences(defendant, defendantResponse);

        log.info("Successfully saved contact preferences for defendant with IDAM ID: {}", currentUserIdamId);
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
    private void updatePartyContactDetails(PartyEntity party, PossessionClaimResponse defendantResponse) {
        boolean updated = false;

        if (StringUtils.isNotBlank(defendantResponse.getPhoneNumber())) {
            party.setPhoneNumber(defendantResponse.getPhoneNumber());
            updated = true;
            log.debug("Updated phone number for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantResponse.getEmail())) {
            party.setEmailAddress(defendantResponse.getEmail());
            updated = true;
            log.debug("Updated email address for party ID: {}", party.getId());
        }

        if (StringUtils.isNotBlank(defendantResponse.getAddress())) {
            AddressEntity address = modelMapper.map(defendantResponse.getAddress(), AddressEntity.class);
            party.setAddress(address);
        }

        if (updated) {
            partyRepository.save(party);
        }
    }

    /**
     * Creates and saves contact preferences entity with null-safe conversion.
     * Defaults null preferences to false (no contact).
     */
    private void createAndSaveContactPreferences(PartyEntity party, PossessionClaimResponse defendantResponse) {
        ContactPreferencesEntity contactPrefs = ContactPreferencesEntity.builder()
            .party(party)
            .contactByEmail(toBooleanOrFalse(defendantResponse.getContactByEmail()))
            .contactByText(toBooleanOrFalse(defendantResponse.getContactByText()))
            .contactByPost(toBooleanOrFalse(defendantResponse.getContactByPost()))
            .contactByPhone(toBooleanOrFalse(defendantResponse.getContactByPhone()))
            .build();

        contactPreferencesRepository.save(contactPrefs);
        log.debug("Saved contact preferences for party ID: {}", party.getId());
    }

    /**
     * Converts YesOrNo to Boolean with safe null handling.
     * Returns false if the input is null (defaults to "no contact" preference).
     *
     * @param yesOrNo the YesOrNo value to convert
     * @return true if YES, false if NO or null
     */
    private Boolean toBooleanOrFalse(YesOrNo yesOrNo) {
        return Optional.ofNullable(YesOrNoConverter.toBoolean(yesOrNo))
            .orElse(false);
    }
}
