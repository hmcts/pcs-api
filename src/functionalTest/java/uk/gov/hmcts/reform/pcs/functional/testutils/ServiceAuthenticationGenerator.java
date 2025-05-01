package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class ServiceAuthenticationGenerator {

    private final String s2sUrl = System.getenv("IDAM_S2S_AUTH_URL");

    public String generate() {
        return generate(TestConstants.PCS_API);
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

        assertThat(SerenityRest.lastResponse().getStatusCode()).isEqualTo(200);

        return SerenityRest.lastResponse().getBody().asString();
    }
}
