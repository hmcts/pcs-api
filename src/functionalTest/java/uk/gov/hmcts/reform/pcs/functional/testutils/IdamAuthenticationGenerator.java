package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

public class IdamAuthenticationGenerator {


    static String idamTokenUrl = System.getenv("IDAM_Bearer_AUTH_URL");
    static String username = System.getenv("IDAM_SYSTEM_USERNAME");
    static String password = System.getenv("IDAM_SYSTEM_USER_PASSWORD");
    static String clientId = "pcs-api";
    static String clientSecret = System.getenv("PCS_API_IDAM_SECRET");
    static String scope = "profile openid roles";
    static String grantType = "password";

    public static String generateToken() {

        Map<String, String> formData = Map.of(
            "username", username,
            "password", password,
            "client_id", clientId,
            "client_secret", clientSecret,
            "scope", scope,
            "grant_type", grantType
        );

        Response response = RestAssured
            .given()
            .baseUri(idamTokenUrl)
            .contentType(APPLICATION_FORM_URLENCODED_VALUE)
            .formParams(formData)
            .when()
            .post()
            .andReturn();


        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to generate IDAM token. Status code: %d. Response: %s",
                response.statusCode(),
                response.asString()
            ));
        }

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.jsonPath().getString("access_token");
    }
}
