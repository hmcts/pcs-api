package uk.gov.hmcts.reform.pcs.service;

import org.awaitility.Awaitility;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.pcs.model.PartyAccessCode;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Service
public class AccessCodeService {

    private static final String PCS_API_HOST = "http://localhost:3206";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String serviceAuthorisation;

    public AccessCodeService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceAuthorisation = generateTestS2SToken("pcs_frontend");
    }

    public List<PartyAccessCode> waitForAccessCodes(long caseReference) {
        return Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(500))
            .ignoreExceptions()
            .until(() -> getAccessCodesForCase(caseReference), not(empty()));
    }

    public void linkUserToCase(long caseReference, String accessCode, String authorisation) {
        RestClient restClient = RestClient
            .create(PCS_API_HOST);

        ValidateAccessCodeRequest validateAccessCodeRequest = new ValidateAccessCodeRequest();
        validateAccessCodeRequest.setAccessCode(accessCode);

        ResponseEntity<Void> responseEntity = restClient
            .post()
            .uri("/cases/{caseReference}/validate-access-code", caseReference)
            .contentType(APPLICATION_JSON)
            .header("Authorization", authorisation)
            .header("ServiceAuthorization", serviceAuthorisation)
            .body(validateAccessCodeRequest)
            .retrieve()
            .toBodilessEntity();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private List<PartyAccessCode> getAccessCodesForCase(long caseReference) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("caseReference", caseReference);

        return jdbcTemplate.query(
            """
                SELECT tsac.party_id, tsac.plaintext_code
                FROM testing_support_access_code tsac
                JOIN pcs_case pcs on pcs.id = tsac.case_id
                WHERE pcs.case_reference = :caseReference
                """,
            namedParameters,
            (rs, rowNum) -> PartyAccessCode.builder()
                .partyId(rs.getObject("party_id", UUID.class))
                .accessCode(rs.getString("plaintext_code"))
                .build()
        );
    }

}
