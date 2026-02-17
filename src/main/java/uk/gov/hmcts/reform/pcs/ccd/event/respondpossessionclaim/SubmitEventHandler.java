package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ContactPreferenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ContactPreferenceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ImmutablePartyFieldValidator immutableFieldValidator;
    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final AddressMapper addressMapper;
    private final DefendantResponseRepository defendantResponseRepository;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);

        SubmitResponse<State> validationError = validate(caseData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        // Check if defendant clicked "Submit" (final) or "Save and come back later" (draft)
        YesOrNo submitFlag = Optional.ofNullable(caseData.getSubmitDraftAnswers())
            .orElse(YesOrNo.NO);

        //Always submit draft data, even if we are doing the 'final' submission
        SubmitResponse<State> draftSubmitResponse = processDraftSubmit(caseReference, caseData);
        return submitFlag.toBoolean() ? processFinalSubmit(caseReference) : draftSubmitResponse;
    }

    private SubmitResponse<State> validate(PCSCase caseData, long caseReference) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        if (response == null) {
            log.error("Submit failed for case {}: possessionClaimResponse is null", caseReference);
            return error("Invalid submission: missing response data");
        }

        return null;
    }

    private SubmitResponse<State> validateContactPreferences(PCSCase draftData, long caseReference) {
        PossessionClaimResponse response = draftData.getPossessionClaimResponse();
        DefendantContactDetails contactDetails = response.getDefendantContactDetails();

        if (contactDetails == null || contactDetails.getParty() == null) {
            log.error("Final submit rejected for case {}: missing contact details", caseReference);
            return error("Invalid submission: contact details required for final submission");
        }

        Party party = contactDetails.getParty();
        List<String> errors = new java.util.ArrayList<>();

        validatePreference(response.getDefendantResponses().getContactByEmail(), party.getEmailAddress(),
            "Email address is required when email contact preference is selected",
            "contactByEmail", caseReference, errors);

        // Text and phone both require phone number - validate once if either is selected
        boolean phoneRequired = isPreferenceEnabled(response.getDefendantResponses().getContactByText())
            || isPreferenceEnabled(response.getDefendantResponses().getContactByPhone());

        if (phoneRequired && StringUtils.isBlank(party.getPhoneNumber())) {
            errors.add("Phone number is required when text or phone contact preference is selected");
            log.error("Final submit rejected for case {}: text/phone preference YES but phoneNumber missing",
                caseReference);
        }

        return errors.isEmpty() ? null : SubmitResponse.<State>builder().errors(errors).build();
    }

    private void validatePreference(VerticalYesNo preference, String value, String errorMessage,
                                     String fieldName, long caseReference, List<String> errors) {
        if (isPreferenceEnabled(preference) && StringUtils.isBlank(value)) {
            errors.add(errorMessage);
            log.error("Final submit rejected for case {}: {}=YES but value is missing", caseReference, fieldName);
        }
    }

    private boolean isPreferenceEnabled(VerticalYesNo preference) {
        return preference != null && preference.toBoolean();
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference) {
        log.info("Processing final submission for case {}", caseReference);

        try {
            // Step 1: Get current user's IDAM ID
            UUID userId = securityContextService.getCurrentUserId();
            log.debug("Current user ID: {}", userId);

            // Step 2: Load draft data from draft_case_data table
            PCSCase draftData = loadDraftData(caseReference);

            // Step 3: Validate contact preferences against contact details
            SubmitResponse<State> contactValidationError = validateContactPreferences(draftData, caseReference);
            if (contactValidationError != null) {
                return contactValidationError;
            }

            // Step 4: Load case entity and find defendant party
            PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
            PartyEntity defendant = findDefendantParty(caseEntity, userId);

            Optional<DefendantResponseEntity> existingResponse =
                defendantResponseRepository.findByClaimPcsCaseCaseReferenceAndPartyIdamId(
                    caseReference, userId);

            if (existingResponse.isPresent()) {
                log.info("Defendant response already exists for case {} and user {}", caseReference, userId);
                return error("A response has already been submitted for this case.");
            }

            // Step 5: Update defendant details (name, address, phone, email)
            updateDefendantDetails(defendant, draftData);

            // Step 6: Update contact preferences (4 rows)
            updateContactPreferences(defendant, draftData);

            // Step 7: Save defendant response to defendant_response table
            saveDefendantResponse(caseEntity, defendant, draftData);

            // Step 8: Save case entity (cascades party and contact preference changes)
            pcsCaseService.saveCase(caseEntity);
            log.debug("Saved defendant updates for case {}", caseReference);

            // Step 9: Delete draft
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
            log.info("Draft deleted for case {}", caseReference);

            return success();

        } catch (Exception e) {
            log.error("Failed to process final submission for case {}", caseReference, e);
            return error("We couldn't submit your response. Please try again or contact support.");
        }
    }

    private SubmitResponse<State> processDraftSubmit(long caseReference, PCSCase caseData) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        // Validate at least one of contact details or responses is provided
        // Frontend can send: only contact, only responses, or both
        if (response.getDefendantContactDetails() == null
            && response.getDefendantResponses() == null) {
            log.error("Draft submit rejected for case {}: both defendantContactDetails and defendantResponses are null",
                caseReference);
            return error("Invalid submission: no data to save");
        }

        // Validate immutable fields are not sent when contact details provided
        if (response.getDefendantContactDetails() != null
            && response.getDefendantContactDetails().getParty() != null) {

            List<String> violations = immutableFieldValidator.findImmutableFieldViolations(
                response.getDefendantContactDetails().getParty(),
                caseReference
            );

            if (!violations.isEmpty()) {
                log.error("Draft submit rejected for case {}: immutable field violations: {}",
                    caseReference, violations);

                List<String> errors = violations.stream()
                    .map(field -> "Invalid submission: immutable field must not be sent: " + field)
                    .toList();

                return SubmitResponse.<State>builder()
                    .errors(errors)
                    .build();
            }
        }

        try {
            saveDraftToDatabase(caseReference, caseData);
            return success();
        } catch (Exception e) {
            log.error("Failed to save draft for case {}", caseReference, e);
            return error("We couldn't save your response. Please try again or contact support.");
        }
    }

    private void saveDraftToDatabase(long caseReference, PCSCase caseData) {
        PCSCase partialUpdate = buildDefendantOnlyUpdate(caseData);
        draftCaseDataService.patchUnsubmittedEventData(caseReference, partialUpdate, respondPossessionClaim);
    }

    /**
     * Builds partial update containing ONLY defendant's contact details and responses.
     *
     * <p>
     * Why partial? UI may send only defendant responses OR only contact details.
     * Partial update preserves existing fields via deep merge:
     * - Sends: defendant contact details and responses only
     * - patchUnsubmittedEventData merges: preserves existing fields
     * - Result: defendant's new answers merged with existing defendant data
     */
    private PCSCase buildDefendantOnlyUpdate(PCSCase caseData) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .defendantResponses(response.getDefendantResponses())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(defendantAnswersOnly)
            .build();  // Sparse object - other fields preserved by patchUnsubmittedEventData
    }

    private PCSCase loadDraftData(long caseReference) {
        return draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new IllegalStateException(
                String.format("No draft found for case %d", caseReference)
            ));
    }

    private PartyEntity findDefendantParty(PcsCaseEntity caseEntity, UUID userId) {
        return caseEntity.getParties().stream()
            .filter(party -> userId.equals(party.getIdamId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                String.format("No party found for case %d with IDAM ID %s",
                    caseEntity.getCaseReference(), userId)
            ));
    }

    private void updateDefendantDetails(PartyEntity defendant, PCSCase draftData) {
        DefendantContactDetails contactDetails = draftData.getPossessionClaimResponse()
            .getDefendantContactDetails();

        if (contactDetails == null || contactDetails.getParty() == null) {
            return;
        }

        Party draftParty = contactDetails.getParty();

        if (StringUtils.isNotBlank(draftParty.getFirstName())) {
            defendant.setFirstName(draftParty.getFirstName());
        }

        if (StringUtils.isNotBlank(draftParty.getLastName())) {
            defendant.setLastName(draftParty.getLastName());
        }

        if (StringUtils.isNotBlank(draftParty.getEmailAddress())) {
            defendant.setEmailAddress(draftParty.getEmailAddress());
        }

        if (StringUtils.isNotBlank(draftParty.getPhoneNumber())) {
            defendant.setPhoneNumber(draftParty.getPhoneNumber());
        }

        if (draftParty.getPhoneNumberProvided() != null) {
            defendant.setPhoneNumberProvided(draftParty.getPhoneNumberProvided());
        }

        if (draftParty.getAddress() != null) {
            updateOrCreateAddress(defendant, draftParty.getAddress());
        }
    }

    private void updateOrCreateAddress(PartyEntity defendant, AddressUK newAddress) {
        AddressEntity existingAddress = defendant.getAddress();

        if (existingAddress != null) {
            existingAddress.setAddressLine1(newAddress.getAddressLine1());
            existingAddress.setAddressLine2(newAddress.getAddressLine2());
            existingAddress.setAddressLine3(newAddress.getAddressLine3());
            existingAddress.setPostTown(newAddress.getPostTown());
            existingAddress.setCounty(newAddress.getCounty());
            existingAddress.setPostcode(newAddress.getPostCode());
            existingAddress.setCountry(newAddress.getCountry());
        } else {
            defendant.setAddress(addressMapper.toEntity(newAddress));
        }
    }

    private void updateContactPreferences(PartyEntity defendant, PCSCase draftData) {
        PossessionClaimResponse response = draftData.getPossessionClaimResponse();

        if (response == null) {
            return;
        }

        DefendantResponses responses = response.getDefendantResponses();

        updateOrCreateContactPreference(defendant, ContactPreferenceType.EMAIL,
            responses.getContactByEmail());
        updateOrCreateContactPreference(defendant, ContactPreferenceType.TEXT,
            responses.getContactByText());
        updateOrCreateContactPreference(defendant, ContactPreferenceType.POST,
            responses.getContactByPost());
        updateOrCreateContactPreference(defendant, ContactPreferenceType.PHONE,
            responses.getContactByPhone());

        log.debug("Updated contact preferences for defendant {}", defendant.getId());
    }

    private void updateOrCreateContactPreference(PartyEntity party, ContactPreferenceType type, VerticalYesNo value) {
        if (value == null) {
            return;
        }

        party.getContactPreferences().stream()
            .filter(pref -> pref.getPreferenceType() == type)
            .findFirst()
            .ifPresentOrElse(
                existing -> existing.setEnabled(value.toBoolean()),
                () -> {
                    ContactPreferenceEntity preference = ContactPreferenceEntity.builder()
                        .party(party)
                        .preferenceType(type)
                        .enabled(value.toBoolean())
                        .build();
                    party.getContactPreferences().add(preference);
                });
    }

    private void saveDefendantResponse(PcsCaseEntity caseEntity, PartyEntity defendant, PCSCase draftData) {
        DefendantResponses responses = draftData.getPossessionClaimResponse().getDefendantResponses();

        if (responses == null) {
            log.debug("No defendant responses to save for defendant {}", defendant.getId());
            return;
        }

        ClaimEntity claim = caseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                String.format("No claim found for case %d", caseEntity.getCaseReference())
            ));

        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .claim(claim)
            .party(defendant)
            .tenancyTypeCorrect(responses.getTenancyTypeCorrect())
            .tenancyStartDateCorrect(responses.getTenancyStartDateCorrect())
            .oweRentArrears(responses.getOweRentArrears())
            .rentArrearsAmount(responses.getRentArrearsAmount())
            .noticeReceived(responses.getNoticeReceived())
            .noticeReceivedDate(responses.getNoticeReceivedDate())
            .receivedFreeLegalAdvice(responses.getReceivedFreeLegalAdvice())
            .build();

        defendantResponseRepository.save(defendantResponse);
        log.debug("Saved defendant response for defendant {} in case {}",
            defendant.getId(), caseEntity.getCaseReference());
    }

    private SubmitResponse<State> success() {
        return SubmitResponse.defaultResponse();
    }

    private SubmitResponse<State> error(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }
}
