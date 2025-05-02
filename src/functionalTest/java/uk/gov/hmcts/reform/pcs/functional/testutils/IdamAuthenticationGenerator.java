package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.CLIENT_ID;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.GRANT_TYPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.SCOPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.ENDPOINT;

public class IdamAuthenticationGenerator {

    private static final String BASE_URL = getEnv("IDAM_Bearer_AUTH_URL");
    private static final String USERNAME = getEnv("IDAM_SYSTEM_USERNAME");
    private static final String PASSWORD = getEnv("IDAM_SYSTEM_USER_PASSWORD");
    private static final String CLIENT_SECRET = getEnv("PCS_API_IDAM_SECRET");

    public static String generateToken() {
        Map<String, String> formData = Map.of(
            "username", USERNAME,
            "password", PASSWORD,
            "client_id", CLIENT_ID,
            "client_secret", CLIENT_SECRET,
            "scope", SCOPE,
            "grant_type", GRANT_TYPE
        );

        Response response = RestAssured
            .given()
            .baseUri(BASE_URL)
            .contentType(APPLICATION_FORM_URLENCODED_VALUE)
            .formParams(formData)
            .post(ENDPOINT);

        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to generate IDAM token. Status code: %d%nResponse: %s",
                response.statusCode(),
                response.prettyPrint()
            ));
        }

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.jsonPath().getString("access_token");
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
