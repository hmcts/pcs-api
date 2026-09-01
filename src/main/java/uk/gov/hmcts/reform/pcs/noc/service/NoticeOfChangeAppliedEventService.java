package uk.gov.hmcts.reform.pcs.noc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

/**
 * Submits the {@code noticeOfChangeApplied} system event as the system user, the same way the payment
 * callback submits {@code claimIssuePayment}. Search visibility is driven by the CaseAccessGroups in the
 * indexed snapshot of the last event, so a representation change only reaches the case list once an
 * event has stored a fresh snapshot.
 */
@Service
@Slf4j
public class NoticeOfChangeAppliedEventService {

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;

    public NoticeOfChangeAppliedEventService(
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        AuthTokenGenerator authTokenGenerator,
        CoreCaseDataApi coreCaseDataApi,
        ObjectMapper objectMapper) {
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.authTokenGenerator = authTokenGenerator;
        this.coreCaseDataApi = coreCaseDataApi;
        this.objectMapper = objectMapper;
    }

    public CaseResource submit(long caseReference, String actorEmail) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = systemUpdateUserTokenProvider.getAuthToken();
        String caseId = String.valueOf(caseReference);
        log.debug("Submitting {} for case {}", noticeOfChangeApplied, caseReference);

        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(
            idamToken, serviceAuthorization, caseId, noticeOfChangeApplied.name());

        CaseDataContent content = CaseDataContent.builder()
            .event(Event.builder().id(noticeOfChangeApplied.name())
                       .summary(isNotBlank(actorEmail) ? "Notice of change by " + actorEmail : null)
                       .build())
            .eventToken(startEventResponse.getToken())
            .data(objectMapper.valueToTree(PCSCase.builder().build()))
            .build();

        return coreCaseDataApi.createEvent(idamToken, serviceAuthorization, caseId, content);
    }
}
