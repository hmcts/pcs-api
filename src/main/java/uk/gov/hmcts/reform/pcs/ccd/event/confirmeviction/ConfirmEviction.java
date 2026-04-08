package uk.gov.hmcts.reform.pcs.ccd.event.confirmeviction;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction.ConfirmEvictionConfigurer;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.confirmEviction;

@Component
@AllArgsConstructor
public class ConfirmEviction implements CCDConfig<PCSCase, State, UserRole> {

    private final ConfirmEvictionConfigurer confirmEvictionConfigurer;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(confirmEviction.name(), this::submit)
                .forAllStates()
                .name("Confirm the eviction details")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();
        confirmEvictionConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }

}
