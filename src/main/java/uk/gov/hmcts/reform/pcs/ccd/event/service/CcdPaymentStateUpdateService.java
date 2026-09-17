package uk.gov.hmcts.reform.pcs.ccd.event.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;

@AllArgsConstructor
@Service
@Slf4j
public class CcdPaymentStateUpdateService {

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

    public CaseResource submitPaymentSuccess(long caseId) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = systemUpdateUserTokenProvider.getAuthToken();
        log.debug("Submitting payment event for case: {}", caseId);
        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(idamToken, serviceAuthorization,
                                                                           String.valueOf(caseId),
                                                                           claimIssuePayment.name());
        CaseDataContent submitContent = CaseDataContent.builder()
            .event(Event.builder().id(claimIssuePayment.name()).build())
            .eventToken(startEventResponse.getToken())
            .data(Map.of())
            .build();
        CaseResource caseResource = coreCaseDataApi.createEvent(idamToken, serviceAuthorization,
                                                                String.valueOf(caseId), submitContent);
        log.debug("CaseResource response : {}", caseResource);
        return caseResource;
    }

}
