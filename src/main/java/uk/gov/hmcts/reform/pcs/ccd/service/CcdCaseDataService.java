package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.confirmCaseDisposal;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.markCaseForDeletion;

@AllArgsConstructor
@Service
@Slf4j
public class CcdCaseDataService {

    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<DraftCasesToDiscard> findExpiredDraftCases(int discardAfterDays) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("discardDaysAfter", discardAfterDays);
        return jdbcTemplate.query(
                """
                   SELECT cd.reference
                   FROM ccd.case_data cd
                   WHERE cd.created_date < now()::date - :discardDaysAfter
                   AND cd.state in ('AWAITING_SUBMISSION_TO_HMCTS', 'PENDING_CASE_ISSUED')
                """,
                namedParameters,
                (rs, rowNum) -> {
                    long caseRef = rs.getLong("reference");
                    return DraftCasesToDiscard.builder()
                            .caseReference(caseRef)
                            .build();
                }
        );
    }

    public CaseResource markCaseForDeletion(long caseRef) {
        log.debug("Marking following case for deletion: {} ", caseRef);

        return performEvent(markCaseForDeletion, caseRef);
    }

    public CaseResource confirmCaseDisposal(long caseRef) {
        log.debug("Confirming disposal for case: {} ", caseRef);

        return performEvent(confirmCaseDisposal, caseRef);
    }

    private CaseResource performEvent(EventId eventId, long caseRef) {
        String serviceAuthorization = authTokenGenerator.generate();
        String idamToken = systemUpdateUserTokenProvider.getAuthToken();

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
    }

    private JsonNode toJsonNode(PCSCase pcsCase) {
        return objectMapper.valueToTree(pcsCase);
    }
}
