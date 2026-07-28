package uk.gov.hmcts.reform.pcs.client;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;

@Component
public class CcdClient {

    private static final EventId CASE_CREATION_EVENT = createPossessionClaim;

    private final CoreCaseDataApi ccdApi;
    private final String serviceAuthorisation;

    public CcdClient(CoreCaseDataApi ccdApi) {
        this.ccdApi = ccdApi;
        this.serviceAuthorisation = generateTestS2SToken("ccd_gw");
    }

    public CaseDetails createCase(PCSCase caseData, String authorisation) {

        StartEventResponse startEventResponse = ccdApi.startCase(
            authorisation,
            serviceAuthorisation,
            CaseType.getCaseType(),
            CASE_CREATION_EVENT.name()
        );

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(CASE_CREATION_EVENT.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        return ccdApi.submitCaseCreation(authorisation, serviceAuthorisation, CaseType.getCaseType(), caseDataContent);
    }

    public CaseResource updateCase(EventId eventId, long caseReference, PCSCase caseData, String authorisation) {

        StartEventResponse startEventResponse = ccdApi.startEvent(
            authorisation,
            serviceAuthorisation,
            Long.toString(caseReference),
            eventId.name()
        );

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        return ccdApi.createEvent(authorisation, serviceAuthorisation, Long.toString(caseReference), caseDataContent);
    }

    public CaseDetails getCaseDetails(long caseReference, String authorisation) {
        return ccdApi.getCase(authorisation, serviceAuthorisation, Long.toString(caseReference));
    }

}
