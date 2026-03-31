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
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardJourneyService;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartDashboardViewHandler implements Start<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final DashboardJourneyService dashboardJourneyService;
    private final ObjectMapper objectMapper;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        State state = State.CASE_ISSUED;

        log.info("DashboardView START invoked for caseReference={}, assumedState={}", caseReference, state);

        PCSCase submittedCaseData = eventPayload.caseData();

        Optional<PCSCase> draftForRespondPossession = getDraft(caseReference, respondPossessionClaim);

        boolean hasDraft = draftForRespondPossession.isPresent();
        log.info("DashboardView START loaded data for caseReference={}, hasRespondPossessionDraft={}",
                 caseReference, hasDraft);

        List<DashboardNotification> notifications = dashboardJourneyService.computeNotifications(
            caseReference,
            state,
            submittedCaseData,
            draftForRespondPossession
        );

        log.info("DashboardView START computed {} dashboard notification(s) for caseReference={}",
                 notifications.size(), caseReference);

        String payloadJson = toPayloadJson(caseReference, notifications);

        log.info("DashboardView START serialised dashboard payload for caseReference={} (length={} chars)",
                 caseReference, payloadJson.length());

        submittedCaseData.setCaseTitleMarkdown(payloadJson);
        return submittedCaseData;
    }

    private Optional<PCSCase> getDraft(long caseReference, EventId eventId) {
        try {
            log.info("DashboardView START attempting to load draft for caseReference={}, eventId={}",
                     caseReference, eventId);
            return draftCaseDataService.getUnsubmittedCaseData(caseReference, eventId);
        } catch (Exception e) {
            log.warn("Failed to load draft case data for caseReference={} eventId={}", caseReference, eventId, e);
            return Optional.empty();
        }
    }

    private String toPayloadJson(long caseReference, List<DashboardNotification> notifications) {
        DashboardPayload payload = new DashboardPayload(notifications);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise dashboard payload for caseReference={}", caseReference, e);
            return "{\"notifications\":[]}";
        }
    }

    private record DashboardPayload(List<DashboardNotification> notifications) {
    }
}

