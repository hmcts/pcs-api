package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitDefendantResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitDefendantResponse implements CCDConfig<PCSCase, State, UserRole> {
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(submitDefendantResponse.name(), this::submit, this::start)
            // TODO: HDPI-3580 - Revert to .forState(State.CASE_ISSUED) once payments flow is implemented
            // Temporarily enabled for all states to allow testing before case submission/payment
            .forAllStates()
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Defendant Response Submission")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        UserInfo userInfo = securityContextService.getCurrentUserDetails();
        UUID authenticatedUserId = UUID.fromString(userInfo.getUid());
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<Defendant> defendants = pcsCaseEntity.getDefendants();

        if (defendants == null || defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        Defendant matchedDefendant = defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamUserId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });

        Party party = Party.builder()
            .forename(matchedDefendant.getFirstName())
            .surname(matchedDefendant.getLastName())
            .contactAddress(matchedDefendant.getCorrespondenceAddress())
            .idamId(matchedDefendant.getIdamUserId())
            .build();

        DefendantResponse defendantResponse = DefendantResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = eventPayload.caseData();
        caseData.setDefendantResponse(defendantResponse);

        // Create filtered PCSCase with ONLY defendantResponse for draft storage
        PCSCase filteredDraft = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .build();

        // Save filtered draft (not full case data)
        draftCaseDataService.patchUnsubmittedEventData(
            caseReference, filteredDraft, EventId.submitDefendantResponse, authenticatedUserId);

        return filteredDraft;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Update Draft Data for Defendant Response, Case Reference: {}", eventPayload.caseReference());

        long caseReference = eventPayload.caseReference();
        DefendantResponse defendantResponse = eventPayload.caseData().getDefendantResponse();
        YesOrNo isFinalSubmit = eventPayload.caseData().getSubmitDraftAnswers();
        UUID userId = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        if (defendantResponse != null && isFinalSubmit != null) {
            if (isFinalSubmit.toBoolean()) {
                //find draft data using idam user and case referecce and event

                //Store defendant response to database
                //This will be implemented in a future ticket.
                //Note that defendants will be stored in a list
            } else {
                // Create filtered PCSCase with ONLY defendantResponse for draft storage
                PCSCase filteredDraft = PCSCase.builder()
                    .defendantResponse(defendantResponse)
                    .build();

                // Update draft with filtered data
                draftCaseDataService.patchUnsubmittedEventData(
                    caseReference, filteredDraft, submitDefendantResponse, userId);
            }
        }
        return SubmitResponse.defaultResponse();
    }
}
