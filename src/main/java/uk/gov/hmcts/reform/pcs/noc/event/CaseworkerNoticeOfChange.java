package uk.gov.hmcts.reform.pcs.noc.event;

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

@Component
@AllArgsConstructor
@Slf4j
public class CaseworkerNoticeOfChange implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("caseworkerNoticeOfChange", this::submit)
            .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
            .name("Notice of change")
            .grant(Permission.CRU, UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Received: {}", eventPayload);
        return SubmitResponse.<State>builder().state(State.CASE_ISSUED).build();
    }

}
