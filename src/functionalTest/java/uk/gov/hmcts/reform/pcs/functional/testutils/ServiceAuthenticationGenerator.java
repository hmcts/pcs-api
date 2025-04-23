package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class ServiceAuthenticationGenerator {

    private final String s2sUrl = System.getenv("IDAM_S2S_AUTH_URL");
    private final String microservice = "pcs_api";

    public String generate() {
        return generate(this.microservice);
    }

    public String generate(final String microservice) {
        final Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(s2sUrl)
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(Map.of("microservice", microservice))
            .when()
            .post("/testing-support/lease")
            .andReturn();

        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to generate S2S token for '%s'. Status code: %d. Response: %s",
                microservice,
                response.statusCode(),
                response.asString()
            ));
        }

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.getBody().asString();
    }
}
