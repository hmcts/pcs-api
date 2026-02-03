package uk.gov.hmcts.reform.pcs.functional.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient.UserType.citizenUser;
import static uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient.UserType.systemUser;
import static uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient.UserType.solicitorUser;

public class ApiSteps {

    private RequestSpecification request;
    private Response response;
    private static final String baseUrl = System.getenv("TEST_URL");
    public static String pcsApiS2sToken;
    private static String pcsFrontendS2sToken;
    private static String unauthorisedS2sToken;
    public static String systemUserIdamToken;
    public static String citizenUserIdamToken;
    public static String solicitorUserIdamToken;
    Long caseId;

    @Step("Generate S2S tokens")
    public static void setUp() {
        ServiceAuthenticationGenerator serviceAuthenticationGenerator = new ServiceAuthenticationGenerator();
        pcsApiS2sToken = serviceAuthenticationGenerator.generate();
        pcsFrontendS2sToken = serviceAuthenticationGenerator.generate(TestConstants.PCS_FRONTEND);
        unauthorisedS2sToken = serviceAuthenticationGenerator.generate(TestConstants.CIVIL_SERVICE);

        systemUserIdamToken = PcsIdamTokenClient.generateToken(systemUser);
        citizenUserIdamToken = PcsIdamTokenClient.generateToken(citizenUser);
        solicitorUserIdamToken = PcsIdamTokenClient.generateToken(solicitorUser);

        SerenityRest.given().baseUri(baseUrl);
    }

    @Step("a request is prepared with appropriate values")
    public void requestIsPreparedWithAppropriateValues() {
        request = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON);
    }

    @Step("the request contains a valid service token for {0}")
    public void theRequestContainsValidServiceToken(String microservice) {
        final Map<String, String> serviceTokens = Map.of(
            TestConstants.PCS_API, pcsApiS2sToken,
            TestConstants.PCS_FRONTEND, pcsFrontendS2sToken
        );

        if (!serviceTokens.containsKey(microservice.toLowerCase())) {
            throw new IllegalArgumentException("Unknown microservice: " + microservice);
        }

        String validS2sToken = serviceTokens.get(microservice.toLowerCase());
        request = request.header(TestConstants.SERVICE_AUTHORIZATION, validS2sToken);
    }

    @Step("the request contains an unauthorised service token")
    public void theRequestContainsUnauthorisedServiceToken() {
        request = request.header(TestConstants.SERVICE_AUTHORIZATION, unauthorisedS2sToken);
    }

    @Step("the request contains an expired service token")
    public void theRequestContainsExpiredServiceToken() {
        String expiredS2sToken = TestConstants.EXPIRED_S2S_TOKEN;
        request = request.header(TestConstants.SERVICE_AUTHORIZATION, expiredS2sToken);
    }

    @Step("the request contains the path parameter {0} as {1}")
    public void theRequestContainsThePathParameter(String pathParam, String value) {
        request = request.pathParam(pathParam, value);
    }


    @Step("a call is submitted to the {0} endpoint using a {1} request")
    public void callIsSubmittedToTheEndpoint(String resource, String method) {
        Endpoints resourceAPI = Endpoints.valueOf(resource);

        response = switch (method.toUpperCase()) {
            case "POST" -> request.when().post(resourceAPI.getResource());
            case "GET" -> request.when().get(resourceAPI.getResource());
            case "DELETE" -> request.when().delete(resourceAPI.getResource());
            default -> throw new IllegalStateException("Unexpected value: " + method.toUpperCase());
        };
    }

    @Step("Check status code is {0}")
    public void checkStatusCode(int statusCode) {
        if (response == null) {
            throw new IllegalStateException("No response available. Did you call callIsSubmittedToTheEndpoint first?");
        }
        response.then().assertThat().statusCode(statusCode);
    }

    @Step("the response body contains {0} as a string: {1}")
    public void theResponseBodyContainsAString(String attribute, String value) {
        if (response == null) {
            throw new IllegalStateException("No response available. Did you call callIsSubmittedToTheEndpoint first?");
        }
        response.then().assertThat().body(attribute, Matchers.equalTo(value));
    }

    @Step("the response body matches the expected list")
    public void theResponseBodyMatchesTheExpectedList(List<Map<String, Object>> expectedList) {
        SerenityRest.then().assertThat().body("", Matchers.equalTo(expectedList));
    }

    @Step("the response body is an empty array")
    public void theResponseBodyIsAnEmptyArray() {
        SerenityRest.then()
            .assertThat()
            .body("", Matchers.hasSize(0));
    }

    @Step("the response body matches the expected response")
    public void theResponseBodyMatchesTheExpectedResponse(String expectedResponsePath) throws IOException {
        String expectedResponse = new String(Files.readAllBytes(Paths.get(expectedResponsePath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedJson = mapper.readTree(expectedResponse);
        JsonNode actualJson = mapper.readTree(SerenityRest.lastResponse().asString());
        assertEquals(expectedJson, actualJson);
    }

    @Step("the request contains a valid IDAM token")
    public void theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType user) {

        String userToken = switch (user) {
            case systemUser -> systemUserIdamToken;
            case citizenUser -> citizenUserIdamToken;
            case solicitorUser -> solicitorUserIdamToken;
        };

        request = request.header(TestConstants.AUTHORIZATION, "Bearer " + userToken);
    }

    @Step("the request contains an expired IDAM token")
    public void theRequestContainsExpiredIdamToken() {
        String expiredIdamToken = TestConstants.EXPIRED_IDAM_TOKEN;
        request = request.header(TestConstants.AUTHORIZATION, "Bearer " + expiredIdamToken);
    }

    @Step("the request contains the query parameter {0} as {1}")
    public void theRequestContainsTheQueryParameter(String queryParam, String value) {
        request = request.queryParam(queryParam, value);
    }

    @Step("the request contains a request body")
    public void theRequestContainsBody(Object body) {
        request = request.body(body);
    }

    @Step("a case for {0} is created")
    public Long ccdCaseIsCreated(String legislativeCountry) {
        caseId = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + solicitorUserIdamToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
            .pathParam("legislativeCountry", legislativeCountry)
            .when()
            .post(Endpoints.CreateTestCase.getResource())
            .then()
            .statusCode(201)
            .extract()
            .path("caseId");

        return caseId;
    }

    @Step("a pin is fetched")
    public String accessCodeIsFetched(Long caseReference) {
        Map<String, Object> pins = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
            .pathParam("caseReference", caseReference)
            .when()
            .get(Endpoints.GetPins.getResource())
            .then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<Map<String, Object>>() {});

        return pins.keySet().iterator().next();
    }
}
