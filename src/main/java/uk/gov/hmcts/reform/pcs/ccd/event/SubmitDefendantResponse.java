package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitDefendantResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitDefendantResponse implements CCDConfig<PCSCase, State, UserRole> {
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(submitDefendantResponse.name(), this::submit)
            .forStateTransition(State.AWAITING_SUBMISSION_TO_HMCTS, State.AWAITING_SUBMISSION_TO_HMCTS)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Draft case")
            .description("Save Draft Case")
            .grant(Permission.CRU, UserRole.CITIZEN);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Update Draft Data for Defendant Response, Case Reference: {}", eventPayload.caseReference());

        long caseReference = eventPayload.caseReference();
//        DefendantResponse defendantResponse = eventPayload.caseData().getDefendantResponse();

//        if (defendantResponse.getDefendantResponseFinalSubmit().toBoolean()) {
          if(false) {
            //Store defendant response to database
            //This will be implemented in a future ticket.
            //Note that defendants will be stored in a list
        } else {
            draftCaseDataService.patchUnsubmittedCaseData(caseReference,  PCSCase.builder().build(), EventId.submitDefendantResponse);
        }
        return SubmitResponse.defaultResponse();
    }
}
