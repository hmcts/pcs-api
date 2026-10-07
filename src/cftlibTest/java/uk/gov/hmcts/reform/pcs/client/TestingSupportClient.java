package uk.gov.hmcts.reform.pcs.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.pcs.testingsupport.model.PartyEmail;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Component
public class TestingSupportClient {

    private static final String BASE_URL = "http://localhost:3206/testing-support";

    private final String serviceAuthorisation;
    private final RestClient restClient;

    public TestingSupportClient() {
        this.restClient = RestClient.create(BASE_URL);
        this.serviceAuthorisation = generateTestS2SToken("pcs_api");
    }

    public void setPartyEmail(PartyEmail partyEmail, String authorisation) {
        restClient
            .post()
            .uri("/party/{partyId}/email-address", partyEmail.getPartyId())
            .contentType(APPLICATION_JSON)
            .header("Authorization", authorisation)
            .header("ServiceAuthorization", serviceAuthorisation)
            .body(partyEmail)
            .retrieve()
            .toBodilessEntity();
    }

}
