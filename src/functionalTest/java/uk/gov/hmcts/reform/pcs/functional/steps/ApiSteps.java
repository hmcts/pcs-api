package uk.gov.hmcts.reform.pcs.functional.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;

import java.util.Map;

public class ApiSteps {

    RequestSpecification request;
    Response response;

    private final String baseUrl = System.getenv("TEST_URL");
    private String pcsApiS2sToken;
    private String pcsFrontendS2sToken;
    private String unauthorisedS2sToken;

    public void setUp() {

        ServiceAuthenticationGenerator serviceAuthenticationGenerator = new ServiceAuthenticationGenerator();
        pcsApiS2sToken = serviceAuthenticationGenerator.generate();
        pcsFrontendS2sToken = serviceAuthenticationGenerator.generate("pcs_frontend");
        unauthorisedS2sToken = serviceAuthenticationGenerator.generate("civil_service");

        SerenityRest.given().baseUri(baseUrl);
    }

    @Step("a request is prepared with appropriate values")
    public void requestIsPreparedWithAppropriateValues() {
        request = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header("Authorization", "");
    }

    @Step("the request contains a valid service token for {0}")
    public void theRequestContainsValidServiceToken(String microservice) {
        final Map<String, String> serviceTokens = Map.of(
            "pcs_api", pcsApiS2sToken,
            "pcs_frontend", pcsFrontendS2sToken
        );

        String validS2sToken = serviceTokens.get(microservice.toLowerCase());
        request = request.request().header("ServiceAuthorization", validS2sToken);
    }

    @Step("the request contains an unauthorised service token")
    public void theRequestContainsUnauthorisedServiceToken() {
        request = request.request().header("ServiceAuthorization", unauthorisedS2sToken);
    }

    @Step("the request contains an expired service token")
    public void theRequestContainsExpiredServiceToken() {
        String expiredS2sToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwY3NfYXBpIiwiZXhwIjoxNzQ0MjI0NTgyfQ.5vd6i9FgZOXaOc"
            + "Wnlz4qAUN4Zutf4wyjoIU0DjmA_1G2FZm2uR_zKkl6lz4jc9_Trrf_cqU3Wi2B9GK5vD8LpQ";
        request = request.request().header("ServiceAuthorization", expiredS2sToken);
    }

    @Step("the request contains the path parameter {0} as {1}")
    public void theRequestContainsThePathParameter(String pathParam, String value) {
        request = request.pathParam(pathParam,value);
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
        response.then().assertThat().statusCode(statusCode);
    }

    @Step("the response body contains {string} as {string}")
    public void theResponseBodyContains(String attribute, String value) {
        response.then().assertThat().body(attribute, Matchers.equalTo(value));
    }
}
