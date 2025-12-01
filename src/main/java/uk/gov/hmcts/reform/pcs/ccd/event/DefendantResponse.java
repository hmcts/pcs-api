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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CREATOR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.defendantResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class DefendantResponse implements CCDConfig<PCSCase, State, UserRole> {
    private final DraftCaseDataService draftCaseDataService;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(defendantResponse.name(), this::submit)
            .forStateTransition(AWAITING_FURTHER_CLAIM_DETAILS, AWAITING_FURTHER_CLAIM_DETAILS)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Draft case")
            .description("Save Draft Case")
            .grant(Permission.CRU, CREATOR)
            .grant(Permission.R, UserRole.PCS_CASE_WORKER);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Draft Case Data {}", eventPayload.caseReference());

        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        draftCaseDataService.patchUnsubmittedCaseData(caseReference, caseData, defendantResponse);


        PCSCase pcsCase = eventPayload.caseData();
        return SubmitResponse.defaultResponse();
    }

}
