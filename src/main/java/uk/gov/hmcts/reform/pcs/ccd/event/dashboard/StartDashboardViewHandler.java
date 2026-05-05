package uk.gov.hmcts.reform.pcs.ccd.event.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardJourneyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartDashboardViewHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final DefendantAccessValidator accessValidator;
    private final SecurityContextService securityContextService;
    private final DashboardJourneyService dashboardJourneyService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.debug("DashboardView START invoked for caseReference={}", caseReference);

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        accessValidator.validateAndGetDefendant(caseEntity, securityContextService.getCurrentUserId());

        PCSCase submittedCaseData = eventPayload.caseData();

        DashboardData dashboardData = dashboardJourneyService.computeDashboardData(
            caseReference,
            submittedCaseData
        );

        log.debug(
            "DashboardView START caseReference={} notifications={} taskGroups={}",
            caseReference,
            dashboardData.getNotifications().size(),
            dashboardData.getTaskGroups().size()
        );

        submittedCaseData.setDashboardData(dashboardData);
        return submittedCaseData;
    }
}
