package uk.gov.hmcts.reform.pcs.client;

import org.springframework.cloud.openfeign.EnableFeignClients;
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
import uk.gov.hmcts.reform.pcs.model.MidEventCaseDataContent;

import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;

@Component
@EnableFeignClients(clients = CcdValidateApi.class)
public class CcdClient {

    private static final EventId CASE_CREATION_EVENT = createPossessionClaim;

    private final CoreCaseDataApi ccdApi;
    private final CcdValidateApi ccdValidateApi;

    private final String serviceAuthorisation;

    public CcdClient(CoreCaseDataApi ccdApi, CcdValidateApi ccdValidateApi) {
        this.ccdApi = ccdApi;
        this.ccdValidateApi = ccdValidateApi;
        this.serviceAuthorisation = generateTestS2SToken("ccd_gw");
    }

    public StartEventResponse startEvent(EventId eventId, long caseReference, String authorisation) {
        return ccdApi.startEvent(
            authorisation,
            serviceAuthorisation,
            Long.toString(caseReference),
            eventId.name()
        );
    }

    public CaseDetails midEvent(EventId eventId,
                                long caseReference,
                                String pageId,
                                PCSCase caseData,
                                String eventToken,
                                String authorisation) {

        MidEventCaseDataContent caseDataContent = MidEventCaseDataContent.builder()
            .caseReference(Long.toString(caseReference))
            .eventData(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(eventToken)
            .build();

        return ccdValidateApi.validate(
            authorisation,
            serviceAuthorisation,
            CaseType.getCaseType(),
            pageId,
            caseDataContent
        );
    }

    public CaseResource submitEvent(EventId eventId,
                                    long caseReference,
                                    PCSCase caseData,
                                    String eventToken,
                                    String authorisation
                                    ) {

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(eventToken)
            .build();

        return ccdApi.createEvent(authorisation, serviceAuthorisation, Long.toString(caseReference), caseDataContent);
    }

    /**
     * Combines the starting and submittion of a case creation event.
     * @param caseData The data with which to create the case
     * @param authorisation The IDAM token for the user who is creating the case
     * @return The created case details
     */
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

    /**
     * Combines the starting and submittion of a case update event.
     * @param caseData The data with which to update the case
     * @param authorisation The IDAM token for the user who is updating the case
     * @return The updated case resource
     */
    public CaseResource updateCase(EventId eventId, long caseReference, PCSCase caseData, String authorisation) {
        StartEventResponse startEventResponse = startEvent(eventId, caseReference, authorisation);
        return submitEvent(eventId, caseReference, caseData, startEventResponse.getToken(), authorisation);
    }


    public CaseDetails getCaseDetails(long caseReference, String authorisation) {
        return ccdApi.getCase(authorisation, serviceAuthorisation, Long.toString(caseReference));
    }

}
