package uk.gov.hmcts.reform.pcs.ccd.event.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardJourneyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartDashboardViewHandler implements Start<PCSCase, State> {

    /**
     * TEMP (HDPI-5421): remove temporary state fields on DashboardData when CCD exposes case state.
     */
    private static final String TEMP_STATE_RESOLUTION_NOTE =
        "CCD state is not on EventPayload; dashboardView uses forAllStates(); "
            + "logic uses appliedCaseState below until wired.";

    private final PcsCaseService pcsCaseService;
    private final DefendantAccessValidator accessValidator;
    private final SecurityContextService securityContextService;
    private final DashboardJourneyService dashboardJourneyService;
    private final ObjectMapper objectMapper;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("DashboardView START invoked for caseReference={}", caseReference);

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        accessValidator.validateAndGetDefendant(caseEntity, securityContextService.getCurrentUserId());

        PCSCase submittedCaseData = eventPayload.caseData();

        // TODO: HDPI-5421 - CCD state is not available in EventPayload; default to CASE_ISSUED for now
        State state = State.CASE_ISSUED;

        // DashboardData dashboardData = dashboardJourneyService.computeDashboardData(submittedCaseData, state);
        //
        // log.info("DashboardView START computed {} notification(s), {} taskGroup(s) for caseReference={}",
        //          dashboardData.notifications().size(), dashboardData.taskGroups().size(), caseReference);

        DashboardData dashboardData = dashboardJourneyService.computeDashboardData(
            submittedCaseData,
            state,
            state.name(),
            TEMP_STATE_RESOLUTION_NOTE
        );

        log.info(
            "DashboardView START caseReference={} appliedCaseState={} stateResolution={} "
                + "notifications={} taskGroups={}",
            caseReference,
            dashboardData.appliedCaseState(),
            dashboardData.stateResolution(),
            dashboardData.notifications().size(),
            dashboardData.taskGroups().size()
        );

        submittedCaseData.setDashboardData(serializeDashboardData(caseReference, dashboardData));
        return submittedCaseData;
    }

    private String serializeDashboardData(long caseReference, DashboardData dashboardData) {
        try {
            return objectMapper.writeValueAsString(dashboardData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise dashboard data for caseReference={}", caseReference, e);
            return "{}";
        }
    }
}
