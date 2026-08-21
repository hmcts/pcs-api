package uk.gov.hmcts.reform.pcs.ccd.event.casedeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.SYSTEM_USER;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.markCaseForDeletion;

@Slf4j
@Component
@AllArgsConstructor
public class MarkCaseForDeletion implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(markCaseForDeletion.name(), this::submit)
            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.PENDING_CASE_ISSUED)
            .name("Discard unissued cases")
            .showCondition(NEVER_SHOW)
            .grant(Permission.CRU, SYSTEM_USER);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.<State>builder()
            .state(State.DRAFT_DISCARDED)
            .build();
    }
}
