package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.rest.SerenityRest;
import io.restassured.response.Response;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers.UserType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.CLIENT_ID;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.SCOPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.GRANT_TYPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.ENDPOINT;
import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

public class PcsIdamTokenClient {

    public static final Map<String, Map<String, String>> usersDetails = new HashMap<>();
    private static final String BASE_URL = getEnv("IDAM_API_URL");
    private static final String GENERIC_PASSWORD = TestConstants.GENERIC_PASSWORD;
    private static final String CLIENT_SECRET = getEnv("PCS_API_IDAM_SECRET");

    public static String generateToken(UserType user) {
        String username = TestUsers.getUsername(user);
        if (TestUsers.isCitizenUser(user)) {
            username = CitizenUserGenerator.createCitizenUser(username, "PCS", "Citizen");
        }

        String password = switch (user) {
            case systemUser -> TestConstants.IDAM_SYSTEM_PASSWORD;
            default -> GENERIC_PASSWORD;
        };

        return generateTokenWithCredentials(username, password);
    }

    private static String generateTokenWithCredentials(String username, String password) {
        if (usersDetails.containsKey(username)) {
            return usersDetails.get(username).get("access_token");
        }

        Map<String, String> formData = Map.of(
            "username", username,
            "password", password,
            "client_id", CLIENT_ID,
            "client_secret", CLIENT_SECRET,
            "scope", SCOPE,
            "grant_type", GRANT_TYPE
        );

        Response response = SerenityRest
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

        String accessToken = response.jsonPath().getString("access_token");
        usersDetails.put(username, Map.of("access_token", accessToken));

        return accessToken;
    }
}
