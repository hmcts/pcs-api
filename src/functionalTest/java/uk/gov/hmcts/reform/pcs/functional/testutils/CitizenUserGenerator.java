package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.rest.SerenityRest;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

public class CitizenUserGenerator {

    private static final String BASE_URL = getEnv("IDAM_TESTING_SUPPORT_URL");
    private static final String GENERIC_PASSWORD = getEnv("IDAM_PCS_USER_PASSWORD");
    private static final String EMAIL = "pcs-citizen-" + System.currentTimeMillis() + "@test.com";

    private static String generateCitizenEmail() {
        return "pcs-citizen-" + System.currentTimeMillis() + "@test.com";
    }



    static String AUTHORIZATION
        = "Bearer " + PcsIdamTokenClient.generateToken(PcsIdamTokenClient.UserType.systemUser);

    public static String createCitizenUser() {

        String email = generateCitizenEmail();

        Map<String, Object> user = Map.of(
            "email", email,
            "forename", "PCS",
            "surname", "Citizen",
            "roleNames", new String[]{"citizen"}
        );

        Map<String, Object> requestBody = Map.of(
            "password", GENERIC_PASSWORD,
            "user", user
        );

        var response = SerenityRest
            .given()
            .baseUri(BASE_URL)
            .header("Authorization", AUTHORIZATION)
            .contentType(APPLICATION_JSON_VALUE)
            .body(requestBody)
            .post("/test/idam/users");

        int statusCode = response.getStatusCode();
        if (statusCode != 201) {
            throw new RuntimeException(String.format(
                "Failed to generate Citizen User '%s'. Status code: %d.%nResponse: %s",
                email,
                statusCode,
                response.prettyPrint()
            ));
        }

        return email;
    }
}
