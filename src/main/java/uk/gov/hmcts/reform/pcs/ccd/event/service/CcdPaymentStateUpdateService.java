package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CcdPaymentStateUpdateTaskData;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTask.CCD_PAYMENT_DATE_TASK_DESCRIPTOR;

@AllArgsConstructor
@Service
@Slf4j
public class CcdPaymentStateUpdateService {

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;
    private final SchedulerClient schedulerClient;

    public void submitPaymentSuccess(long caseId, EventId eventId) {
        CcdPaymentStateUpdateTaskData taskData = CcdPaymentStateUpdateTaskData.builder()
            .caseReference(caseId).eventId(eventId).build();
        schedulerClient.scheduleIfNotExists(CCD_PAYMENT_DATE_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString()).data(taskData).scheduledTo(Instant.now()));
    }

    public void updateCcd(CcdPaymentStateUpdateTaskData taskData) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = systemUpdateUserTokenProvider.getAuthToken();
        log.debug("Submitting payment event for case: {}", taskData.getCaseReference());
        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(idamToken, serviceAuthorization,
                                                                           String.valueOf(taskData.getCaseReference()),
                                                                           taskData.getEventId().name());
        CaseDataContent submitContent = CaseDataContent.builder()
            .event(Event.builder().id(taskData.getEventId().name()).build())
            .eventToken(startEventResponse.getToken()).data(toJsonNode(PCSCase.builder().build())).build();
        CaseResource caseResource = coreCaseDataApi.createEvent(idamToken, serviceAuthorization,
                                                                String.valueOf(taskData.getCaseReference()),
                                                                submitContent);
        log.debug("CaseResource response : {}", caseResource);
    }

    private JsonNode toJsonNode(PCSCase pcsCase) {
        return objectMapper.valueToTree(pcsCase);
    }

}
