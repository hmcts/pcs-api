package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

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
    private static final String IDAM_SYSTEM_USERNAME = getEnv("IDAM_SYSTEM_USERNAME");
    private static final String IDAM_SYSTEM_PASSWORD = getEnv("IDAM_SYSTEM_USER_PASSWORD");
    private static final String GENERIC_PASSWORD = TestConstants.GENERIC_PASSWORD;
    private static final String SOLICITOR_USERNAME = TestConstants.PCS_SOLICITOR_USER;
    private static final String CLIENT_SECRET = getEnv("PCS_API_IDAM_SECRET");

    public enum UserType {
        systemUser,
        citizenUser,
        solicitorUser
    }

    public static String generateToken(UserType user) {

        String username = switch (user) {
            case systemUser -> IDAM_SYSTEM_USERNAME;
            case citizenUser -> CitizenUserGenerator.createCitizenUser();
            case solicitorUser -> SOLICITOR_USERNAME;
        };

        String password = switch (user) {
            case systemUser -> IDAM_SYSTEM_PASSWORD;
            case citizenUser, solicitorUser -> GENERIC_PASSWORD;
        };

        return generateTokenWithCredentials(username, password);
    }

    // userPath is dot-separated key path into /resources/users.json, e.g. "ORG_SOLICITOR.solicitor1"
    public static String generateTokenForUser(String userPath) {
        String username = TestUsers.getUsername(userPath);
        if (TestUsers.isCitizenUser(userPath)) {
            String forename = TestUsers.getOptionalUserProperty(userPath, "forename");
            String surname = TestUsers.getOptionalUserProperty(userPath, "surname");
            username = CitizenUserGenerator.createCitizenUser(
                username,
                forename == null ? "PCS" : forename,
                surname == null ? "Citizen" : surname
            );
        }
        System.out.println("Generating token for user: " + username);
        return generateTokenWithCredentials(username, GENERIC_PASSWORD);
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

        SerenityRest
            .given()
            .baseUri(BASE_URL)
            .contentType(APPLICATION_FORM_URLENCODED_VALUE)
            .formParams(formData)
            .post(ENDPOINT);

        if (SerenityRest.lastResponse().statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to generate IDAM token. Status code: %d%nResponse: %s",
                SerenityRest.lastResponse().statusCode(),
                SerenityRest.lastResponse().prettyPrint()
            ));
        }

        assertThat(SerenityRest.lastResponse().getStatusCode()).isEqualTo(200);

        String accessToken = SerenityRest.lastResponse().jsonPath().getString("access_token");
//        String userId = SerenityRest.lastResponse().jsonPath().getString("idam");
//        System.out.println(SerenityRest.lastResponse().getBody().prettyPrint());
//        System.out.println("Generated token for user: " + username + ", userId: " + userId);
        usersDetails.put(username, Map.of("access_token", accessToken));

        return accessToken;
    }
}
