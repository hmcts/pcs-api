package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalEventId;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionStart;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.ScheduleEvictionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

/**
 * Event for scheduling an eviction against a warrant.
 */
@Component
@AllArgsConstructor
public class ScheduleEviction implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<ScheduleEvictionStart, ScheduleEvictionRequest> SCHEDULE_EVICTION =
        ExternalEventId.of("ext:scheduleEviction", ScheduleEvictionStart.class, ScheduleEvictionRequest.class);

    static final String CAN_SCHEDULE_EVICTION = "enfShowConditions.scheduleEviction=\"Yes\"";

    private final ScheduleEvictionService scheduleEvictionService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(SCHEDULE_EVICTION, this::submit)
            .forStates(EventStates.scheduleEviction())
            .name("Schedule eviction")
            .showCondition(ShowConditions.and(
                ShowConditions.featureFlagsEnabled(FeatureFlag.EVICTION_SCHEDULING),
                CAN_SCHEDULE_EVICTION
            ))
            .grant(Permission.CRU, UserRole.BAILIFF_ADMIN)
            .grant(Set.of(Permission.R), JUDICIAL_HISTORY_ROLES)
            .onStart(this::start);
    }

    private ExternalStartResponse<ScheduleEvictionStart> start(ExternalStartRequest start) {
        return ExternalStartResponse.started(scheduleEvictionService.getStartDetails(start.caseReference()));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<ScheduleEvictionRequest> submit) {
        scheduleEvictionService.scheduleEviction(submit.caseReference(), submit.payload());
        return ExternalSubmitResponse.accepted("Eviction scheduled", "Scheduled the eviction");
    }
}
