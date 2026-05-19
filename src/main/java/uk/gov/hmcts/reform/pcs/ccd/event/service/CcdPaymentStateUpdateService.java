package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.payment;

@AllArgsConstructor
@Service
@Slf4j
public class CcdPaymentStateUpdateService {

    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;

    public CaseResource submitPaymentSuccess(String caseId) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = idamService.getSystemUserAuthorisation();

        log.debug("Submitting payment event for case: {}", caseId);
        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(idamToken, serviceAuthorization,
                                                                           caseId, payment.name());
        log.debug("StartEventResponse: {}", startEventResponse);
        CaseDataContent submitContent = CaseDataContent.builder().event(Event.builder().id(payment.name()).build())
            .eventToken(startEventResponse.getToken()).data(toJsonNode(PCSCase.builder().build())).build();
        log.debug("submitContent: {}", submitContent);
        CaseResource caseResource = coreCaseDataApi.createEvent(idamToken, serviceAuthorization,
                                                                caseId, submitContent);
        log.debug("CaseResouce response : {}", caseResource);
        return caseResource;
    }

    private JsonNode toJsonNode(PCSCase pcsCase) {
        return objectMapper.valueToTree(pcsCase);
    }

}
