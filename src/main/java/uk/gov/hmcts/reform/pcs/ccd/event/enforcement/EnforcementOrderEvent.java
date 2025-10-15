package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionDelayWarning;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionRisksPosedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.LivingInThePropertyPage;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class EnforcementOrderEvent implements CCDConfig<PCSCase, State, UserRole> {
    // TODO: Business requirements to be agreed on for the conditions when this event can be triggereed

    @Override
    public void configureDecentralised(
            DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(enforceTheOrder.name(), this::submit)
                        .forStateTransition(AWAITING_SUBMISSION_TO_HMCTS, AWAITING_SUBMISSION_TO_HMCTS)
                        .name("Enforce the order")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder).
            add(new EnforcementApplicationPage())
            .add(new LivingInThePropertyPage())
            .add(new EvictionDelayWarning())
            .add(new EvictionRisksPosedPage())
            .add(new EvictionVulnerableAdultsChildrenPage());
    }

    private SubmitResponse submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.builder().build();
    }
}
