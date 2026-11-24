package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkSubmitError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createBulkPossessionClaim;

/**
 * Creates the case for a staged bulk claim by running the system-user-only create event in CCD.
 */
@Slf4j
@Service
public class BulkClaimCcdClient {

    static final String VALIDATION_CODE = "CCD_REJECTED";
    private static final String CALLBACK_ERRORS = "callbackErrors";

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;

    public BulkClaimCcdClient(@Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider tokenProvider,
                              AuthTokenGenerator authTokenGenerator,
                              CoreCaseDataApi coreCaseDataApi,
                              ObjectMapper objectMapper) {
        this.systemUpdateUserTokenProvider = tokenProvider;
        this.authTokenGenerator = authTokenGenerator;
        this.coreCaseDataApi = coreCaseDataApi;
        this.objectMapper = objectMapper;
    }

    public BulkClaimCcdResult createCase(UUID submissionId) {
        try {
            String idamToken = systemUpdateUserTokenProvider.getAuthToken();
            String serviceToken = authTokenGenerator.generate();
            String caseType = CaseType.getCaseType();
            String eventId = createBulkPossessionClaim.name();

            StartEventResponse start = coreCaseDataApi.startCase(idamToken, serviceToken, caseType, eventId);
            CaseDataContent content = CaseDataContent.builder()
                .event(Event.builder().id(eventId).build())
                .eventToken(start.getToken())
                .data(Map.of("bulkClaimSubmissionId", submissionId.toString()))
                .build();

            CaseDetails created = coreCaseDataApi.submitCaseCreation(idamToken, serviceToken, caseType, content);
            return BulkClaimCcdResult.created(created.getId());
        } catch (FeignException.UnprocessableEntity e) {
            log.warn("CCD rejected bulk claim submission {}: {}", submissionId, e.contentUTF8());
            return rejected(e);
        } catch (FeignException e) {
            return failed(submissionId, e, e.status() >= 500 || e.status() < 0);
        } catch (RuntimeException e) {
            return failed(submissionId, e, true);
        }
    }

    private BulkClaimCcdResult failed(UUID submissionId, Exception e, boolean retryable) {
        log.error("Creating the case for bulk claim submission {} failed", submissionId, e);
        return retryable
            ? BulkClaimCcdResult.unavailable()
            : BulkClaimCcdResult.rejected(BulkFailReason.SAVE_FAILED, List.of());
    }

    private BulkClaimCcdResult rejected(FeignException.UnprocessableEntity e) {
        List<BulkSubmitError> parsed = callbackErrors(e.contentUTF8()).stream()
            .map(BulkSubmitError::parse)
            .flatMap(Optional::stream)
            .toList();
        if (parsed.isEmpty()) {
            return BulkClaimCcdResult.rejected(BulkFailReason.SAVE_FAILED, List.of());
        }

        BulkFailReason reason = BulkFailReasonCodes.forFailReasonCode(parsed.getFirst().failReasonCode())
            .orElse(BulkFailReason.SAVE_FAILED);
        List<BulkValidationError> errors = parsed.stream()
            .map(error -> new BulkValidationError(error.path(), VALIDATION_CODE, error.message()))
            .toList();
        return BulkClaimCcdResult.rejected(reason, errors);
    }

    private List<String> callbackErrors(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errors = root.has(CALLBACK_ERRORS) ? root.get(CALLBACK_ERRORS)
                : root.path("details").path(CALLBACK_ERRORS);
            List<String> result = new ArrayList<>();
            errors.forEach(error -> result.add(error.asText()));
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Unable to read CCD error body", e);
            return List.of();
        }
    }
}
