package uk.gov.hmcts.reform.pcs.functional.testutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.rest.SerenityRest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.CLIENT_ID;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.GRANT_TYPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.SCOPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.ENDPOINT;
import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

/**
 * Utility class for creating citizen users in IDAM for functional testing.
 * 
 * This is needed for tests that require different user IDs, such as:
 * - Testing access code already used by another user
 * - Testing user already linked to another defendant
 * 
 * Implementation:
 * 1. Gets system access token (cached, valid for hours)
 * 2. Creates user in IDAM using system token
 * 3. Gets access token for the created user
 */
public class CitizenUserGenerator {

    // Environment variables (matching Jenkins pipeline configuration)
    private static final String IDAM_WEB_PUBLIC_API = getEnvOrDefault("IDAM_WEB_PUBLIC_API_URL", getEnvOrDefault("IDAM_API_URL", null));
    private static final String IDAM_TESTING_SUPPORT_API = getEnvOrDefault("IDAM_TESTING_SUPPORT_API_URL", getEnvOrDefault("IDAM_API_URL", null));
    private static final String IDAM_SYSTEM_USERNAME = getEnv("IDAM_SYSTEM_USERNAME");
    private static final String IDAM_SYSTEM_PASSWORD = getEnv("IDAM_SYSTEM_USER_PASSWORD");
    // Use PCS_API_IDAM_SECRET (from Jenkins vault) instead of PCS_FRONTEND_IDAM_SECRET
    private static final String PCS_API_IDAM_SECRET = getEnv("PCS_API_IDAM_SECRET");
    private static final String DEFAULT_PASSWORD = getEnvOrDefault("IDAM_PCS_USER_PASSWORD", "password");
    
    private static String getEnvOrDefault(String name, String defaultValue) {
        return EnvUtils.getEnvOrDefault(name, defaultValue);
    }

    // System token caching - cached for the entire test run duration
    // Tokens are typically valid for hours, so caching for test run (minutes) is safe
    private static String cachedSystemToken = null;

    /**
     * Creates a new citizen user in IDAM and returns the access token.
     * 
     * @return Access token for the newly created citizen user
     */
    public static String createCitizenUserAndGetToken() {
        // Generate unique user details
        String uniqueId = UUID.randomUUID().toString();
        String email = "test.citizen." + uniqueId + "@test.test";
        String forename = "Test";
        String surname = "Citizen" + uniqueId.substring(0, 8);

        // Create user in IDAM using system token
        createUserInIdam(email, forename, surname, DEFAULT_PASSWORD, List.of("citizen"));

        // Get token for the newly created user
        return getTokenForUser(email, DEFAULT_PASSWORD);
    }

    /**
     * Gets or refreshes the system access token.
     * Token is cached in a static variable for the entire test run duration.
     * Since tests typically run in minutes and tokens are valid for hours,
     * this simple caching approach is sufficient.
     * 
     * @return System access token
     */
    private static synchronized String getOrRefreshSystemToken() {
        // If token is already cached, reuse it
        if (cachedSystemToken != null) {
            return cachedSystemToken;
        }

        // Fetch new token and cache it
        cachedSystemToken = fetchSystemToken();
        return cachedSystemToken;
    }

    /**
     * Fetches a new system access token from IDAM.
     * 
     * @return System access token
     */
    private static String fetchSystemToken() {
        Map<String, String> formData = Map.of(
            "username", IDAM_SYSTEM_USERNAME,
            "password", IDAM_SYSTEM_PASSWORD,
            "client_id", CLIENT_ID,
            "client_secret", PCS_API_IDAM_SECRET,
            "scope", SCOPE,
            "grant_type", GRANT_TYPE
        );

        SerenityRest
            .given()
            .baseUri(IDAM_WEB_PUBLIC_API)
            .contentType("application/x-www-form-urlencoded")
            .formParams(formData)
            .post(ENDPOINT);

        int statusCode = SerenityRest.lastResponse().statusCode();
        if (statusCode != 200) {
            throw new RuntimeException(String.format(
                "Failed to get system access token. Status code: %d%nResponse: %s",
                statusCode,
                SerenityRest.lastResponse().prettyPrint()
            ));
        }

        assertThat(SerenityRest.lastResponse().getStatusCode()).isEqualTo(200);
        return SerenityRest.lastResponse().jsonPath().getString("access_token");
    }

    /**
     * Creates a user in IDAM via the user creation API.
     * Uses the correct endpoint: /test/idam/users
     * 
     * @param email User email
     * @param forename User forename
     * @param surname User surname
     * @param password User password
     * @param roles List of roles to assign (e.g., ["citizen"])
     */
    private static void createUserInIdam(String email, String forename, String surname, 
                                         String password, List<String> roles) {
        // Get system token (cached or refreshed)
        String systemToken = getOrRefreshSystemToken();

        // Build request body matching IDAM API format
        // Use HashMap to support nested structures and List values
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("forename", forename);
        userMap.put("surname", surname);
        userMap.put("roleNames", roles);
        
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("password", password);
        userRequest.put("user", userMap);

        try {
            // Serialize request body to JSON string to ensure proper formatting
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBodyJson = objectMapper.writeValueAsString(userRequest);
            
            SerenityRest
                .given()
                .baseUri(IDAM_TESTING_SUPPORT_API)
                .contentType(APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + systemToken)
                .body(requestBodyJson)
                .when()
                .post("/test/idam/users");

            int statusCode = SerenityRest.lastResponse().statusCode();
            
            // 201 = Created, 409 = Already exists (ok for idempotency)
            if (statusCode != 201 && statusCode != 409) {
                throw new RuntimeException(String.format(
                    "Failed to create user in IDAM. Status code: %d%nRequest body: %s%nResponse: %s",
                    statusCode,
                    requestBodyJson,
                    SerenityRest.lastResponse().prettyPrint()
                ));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user request to JSON", e);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /**
     * Gets an access token for a user by authenticating with IDAM.
     * Uses pcs_frontend_idam_secret for client authentication.
     * 
     * @param email User email
     * @param password User password
     * @return Access token
     */
    private static String getTokenForUser(String email, String password) {
        Map<String, String> formData = Map.of(
            "username", email,
            "password", password,
            "client_id", CLIENT_ID,
            "client_secret", PCS_API_IDAM_SECRET,
            "scope", SCOPE,
            "grant_type", GRANT_TYPE
        );

        SerenityRest
            .given()
            .baseUri(IDAM_WEB_PUBLIC_API)
            .contentType("application/x-www-form-urlencoded")
            .formParams(formData)
            .post(ENDPOINT);

        int statusCode = SerenityRest.lastResponse().statusCode();
        if (statusCode != 200) {
            throw new RuntimeException(String.format(
                "Failed to get token for user %s. Status code: %d%nResponse: %s",
                email,
                statusCode,
                SerenityRest.lastResponse().prettyPrint()
            ));
        }

        assertThat(SerenityRest.lastResponse().getStatusCode()).isEqualTo(200);
        return SerenityRest.lastResponse().jsonPath().getString("access_token");
    }

    /**
     * Creates multiple citizen users and returns their tokens.
     * Useful for tests that need multiple different users.
     * 
     * @param count Number of users to create
     * @return List of access tokens (one per user)
     */
    public static List<String> createMultipleCitizenUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createCitizenUserAndGetToken())
            .toList();
    }

    /**
     * Clears the cached system token.
     * Useful for testing or when token needs to be refreshed manually.
     */
    public static synchronized void clearSystemTokenCache() {
        cachedSystemToken = null;
    }
}
