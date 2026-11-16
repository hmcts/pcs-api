package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
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
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.ConfirmDisposal;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.MarkForDisposal;

/**
 * Triggers CCD retain-and-dispose system events ({@code MarkForDisposal}, {@code ConfirmDisposal}).
 * Prefer the SDK {@code RetainAndDisposePolicy} task for scheduled runs; these helpers remain for
 * transitional callers until that path fully replaces the custom scheduler.
 */
@AllArgsConstructor
@Service
@Slf4j
public class CcdCaseDataDeletionService {

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;
    private final CcdCaseRepository ccdCaseRepository;

    public List<Long> findExpiredDraftCasesBatch(int discardAfterDays, int limit) {
        return ccdCaseRepository.findExpiredDraftCases(discardAfterDays, limit);
    }

    public void deleteCcdCaseData(long caseReference) {
        ccdCaseRepository.deleteCcdCaseData(caseReference);
        log.debug("Deleted case data for case reference: {}", caseReference);
    }

    public CaseResource markCaseForDeletion(long caseRef) {
        log.debug("Marking following case for deletion: {} ", caseRef);

        return performEvent(MarkForDisposal, caseRef);
    }

    public CaseResource confirmCaseDisposal(long caseRef) {
        log.debug("Confirming disposal for case: {} ", caseRef);

        return performEvent(ConfirmDisposal, caseRef);
    }

    private CaseResource performEvent(EventId eventId, long caseRef) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = systemUpdateUserTokenProvider.getAuthToken();
        try {
            StartEventResponse startEventResponse = coreCaseDataApi.startEvent(idamToken, serviceAuthorization,
                    String.valueOf(caseRef),
                    eventId.name());

            CaseDataContent submitContent = CaseDataContent.builder()
                    .event(Event.builder().id(eventId.name()).build())
                    .eventToken(startEventResponse.getToken())
                    .data(toJsonNode(PCSCase.builder().build()))
                    .build();

            CaseResource caseResource = coreCaseDataApi.createEvent(idamToken, serviceAuthorization,
                    String.valueOf(caseRef), submitContent);
            log.debug("Completed event: {} for case: {}", eventId.name(), caseRef);
            return caseResource;
        } catch (FeignException e) {
            log.error("Error running event: {} for case: {}.", eventId.name(), caseRef, e);
            String message = e.getMessage();
            if (message.contains("Case ID is not valid") || message.contains("No case found for reference")) {
                throw new CcdCaseNotFoundException(caseRef);
            }
            throw e;
        }
    }

    private JsonNode toJsonNode(PCSCase pcsCase) {
        return objectMapper.valueToTree(pcsCase);
    }
}
