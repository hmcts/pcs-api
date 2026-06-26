package uk.gov.hmcts.reform.pcs.bulkprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;
import net.serenitybdd.rest.SerenityRest;

public class ServiceAuthenticationGenerator {

    private final String s2sUrl = System.getenv("IDAM_S2S_AUTH_URL");

    public String generate() {
        return generate("pcs_api");
    }

    public String generate(final String microservice) {
        SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .baseUri(s2sUrl)
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(Map.of("microservice", microservice))
            .when()
            .post("/testing-support/lease")
            .andReturn();

        if (SerenityRest.lastResponse().statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to generate S2S token for '%s'. Status code: %d. Response: %s",
                microservice,
                SerenityRest.lastResponse().statusCode(),
                SerenityRest.lastResponse().asString()
            ));
        }

        assertEquals(200, SerenityRest.lastResponse().getStatusCode());

        return SerenityRest.lastResponse().getBody().asString();
    }
}
