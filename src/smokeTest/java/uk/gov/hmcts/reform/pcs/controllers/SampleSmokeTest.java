package uk.gov.hmcts.reform.pcs.controllers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SampleSmokeTest {
    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Autowired
    protected AuthTokenGenerator s2sAuthTokenGenerator;

    @Value("${TEST_URL:http://localhost:8080}")
    private String testUrl;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void smokeTest() {
        String s2sToken = s2sAuthTokenGenerator.generate();
        Response response = given()
            .contentType(ContentType.JSON)
            .header(ServiceAuthFilter.AUTHORISATION, s2sToken)
            .when()
            .get()
            .then()
            .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.asString().startsWith("Welcome"));
    }
}
