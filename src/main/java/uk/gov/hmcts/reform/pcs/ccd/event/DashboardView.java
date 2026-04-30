package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.dashboard.StartDashboardViewHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.dashboard.SubmitDashboardViewHandler;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.dashboardView;

@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardView implements CCDConfig<PCSCase, State, UserRole> {

    private final StartDashboardViewHandler startDashboardViewHandler;
    private final SubmitDashboardViewHandler submitDashboardViewHandler;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(dashboardView.name(), submitDashboardViewHandler, startDashboardViewHandler)
            // TODO: HDPI-5421 - Revist this once we have clarification on the states we need
            .forAllStates()
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Dashboard view")
            .description("Compute dashboard notifications for case journey")
            .grant(Permission.R, UserRole.DEFENDANT);
    }
}

