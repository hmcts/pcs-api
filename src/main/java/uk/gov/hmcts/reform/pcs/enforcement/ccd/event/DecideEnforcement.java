package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.decideenforcement.DecideEnforcementPage;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.decideEnforcement;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Component
@RequiredArgsConstructor
public class DecideEnforcement implements CCDConfig<PCSCase, State, UserRole> {

    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT), "canDecideEnforcement=\"Yes\"");

    static final State[] STATES = {State.CASE_ISSUED, State.PENDING_ENFORCEMENT_ISSUED};

    private final DecideEnforcementStartHandler startHandler;
    private final DecideEnforcementSubmitHandler submitHandler;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(decideEnforcement.name(), submitHandler, startHandler)
                .forStates(STATES)
                .name("Decide enforcement")
                .grant(Permission.CRU, CASEWORKER_ROLES)
                .showCondition(SHOW_CONDITION)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");
        new PageBuilder(eventBuilder).add(new DecideEnforcementPage());
    }
}
