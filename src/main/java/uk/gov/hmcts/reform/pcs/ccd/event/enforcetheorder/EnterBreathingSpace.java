package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.BreathingSpacePageConfigurer;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterBreathingSpace;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
@AllArgsConstructor
public class EnterBreathingSpace implements CCDConfig<PCSCase, State, UserRole> {

    private BreathingSpacePageConfigurer breathingSpacePageConfigurer;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enterBreathingSpace.name(), this::submit)
                .forState(State.CASE_ISSUED)
                .name("Enter breathing space")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showCondition(ShowConditions.featureFlagsEnabled(ENFORCEMENT))
                .showSummary();
        breathingSpacePageConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }

}
