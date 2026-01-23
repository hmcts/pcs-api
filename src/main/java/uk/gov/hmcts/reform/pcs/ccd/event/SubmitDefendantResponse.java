package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ContactPreferencesRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitDefendantResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitDefendantResponse implements CCDConfig<PCSCase, State, UserRole> {
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;
    private final PartyRepository partyRepository;
    private final ContactPreferencesRepository contactPreferencesRepository;
    private final SecurityContextService securityContextService;


    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(submitDefendantResponse.name(), this::submit)
            .forState(State.CASE_ISSUED)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Defendant Response Submission")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Update Draft Data for Defendant Response, Case Reference: {}", eventPayload.caseReference());

        long caseReference = eventPayload.caseReference();
        DefendantResponse defendantResponse = eventPayload.caseData().getDefendantResponse();
        YesOrNo submitDraft = eventPayload.caseData().getSubmitDraftAnswers();

        if (defendantResponse != null && submitDraft != null) {
            if (submitDraft.toBoolean()) {
                //Store defendant response to database
                //This will be implemented in a future ticket.
                //Note that defendants will be stored in a list
                saveDefendantContactPreferences(caseReference, defendantResponse);

            } else {
                draftCaseDataService.patchUnsubmittedEventData(
                    caseReference, defendantResponse, EventId.submitDefendantResponse);
            }
        }
        return SubmitResponse.defaultResponse();
    }

    private void saveDefendantContactPreferences(long caseReference, DefendantResponse defendantResponse) {

        try {
            // Get the current user's IDAM ID (the defendant submitting the response)
            UUID currentUserIdamId = securityContextService.getCurrentUserId();

            if (currentUserIdamId == null) {
                log.error("Cannot save contact preferences: current user IDAM ID is null");
                return;
            }

            // Find the defendant party by their IDAM ID
            PartyEntity defendant = partyRepository.findByIdamId(currentUserIdamId)
                .orElseThrow(() -> new IllegalStateException(
                    "No party found for IDAM ID: " + currentUserIdamId));

            // Update phone number
            if (StringUtils.isNotBlank(defendantResponse.getPhoneNumber())){
                defendant.setPhoneNumber(defendantResponse.getPhoneNumber());
            }

            // Update email address
            if (StringUtils.isNotBlank(defendantResponse.getEmail())){
                defendant.setEmailAddress(defendantResponse.getEmail());
            }

            // Save the updated party entity
            partyRepository.save(defendant);

            // Create contact preferences
            ContactPreferencesEntity contactPrefs = ContactPreferencesEntity.builder()
                .party(defendant)
                .contactByEmail(toBooleanOrFalse(defendantResponse.getContactByEmail()))
                .contactByText(toBooleanOrFalse(defendantResponse.getContactByText()))
                .contactByPost(toBooleanOrFalse(defendantResponse.getContactByPost()))
                .contactByPhone(toBooleanOrFalse(defendantResponse.getContactByPhone()))
                .build();

            // Create contact preferences
            contactPreferencesRepository.save(contactPrefs);

            log.info("Successfully saved contact preferences for defendant with IDAM ID: {} in case: {}",
                     currentUserIdamId, caseReference);

        } catch (Exception ex) {
            log.error("Error saving defendant contact preferences for case: {}", caseReference, ex);
        }
    }

    /**
     * Converts YesOrNo to boolean with safe null handling
     * returns false if input is null
     */
    private Boolean toBooleanOrFalse(YesOrNo yesOrNo) {
        return Optional.ofNullable(YesOrNoConverter.toBoolean(yesOrNo))
            .orElse(false);
    }

}
