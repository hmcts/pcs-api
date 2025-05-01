package uk.gov.hmcts.reform.pcs.functional.steps;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;

import java.util.Map;

public class ApiSteps {

    private RequestSpecification request;
    private final String baseUrl = System.getenv("TEST_URL");
    private String pcsApiS2sToken;
    private String pcsFrontendS2sToken;
    private String unauthorisedS2sToken;

    @Step("Generate S2S tokens")
    public void setUp() {

        ServiceAuthenticationGenerator serviceAuthenticationGenerator = new ServiceAuthenticationGenerator();
        pcsApiS2sToken = serviceAuthenticationGenerator.generate();
        pcsFrontendS2sToken = serviceAuthenticationGenerator.generate(TestConstants.PCS_FRONTEND);
        unauthorisedS2sToken = serviceAuthenticationGenerator.generate(TestConstants.CIVIL_SERVICE);

        SerenityRest.given().baseUri(baseUrl);
    }

    @Step("a request is prepared with appropriate values")
    public void requestIsPreparedWithAppropriateValues() {
        request = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "");
    }

    @Step("the request contains a valid service token for {0}")
    public void theRequestContainsValidServiceToken(String microservice) {
        final Map<String, String> serviceTokens = Map.of(
            TestConstants.PCS_API, pcsApiS2sToken,
            TestConstants.PCS_FRONTEND, pcsFrontendS2sToken
        );

        if (!serviceTokens.containsKey(microservice.toLowerCase())) {
            throw new IllegalArgumentException(TestConstants.UNKNOWN_MICROSERVICE + microservice);
        }

        String validS2sToken = serviceTokens.get(microservice.toLowerCase());
        request = request.request().header(TestConstants.SERVICE_AUTHORIZATION, validS2sToken);
    }

    @Step("the request contains an unauthorised service token")
    public void theRequestContainsUnauthorisedServiceToken() {
        request = request.request().header(TestConstants.SERVICE_AUTHORIZATION, unauthorisedS2sToken);
    }

    @Step("the request contains an expired service token")
    public void theRequestContainsExpiredServiceToken() {
        String expiredS2sToken = TestConstants.EXPIRED_S2S_TOKEN;
        request = request.request().header(TestConstants.SERVICE_AUTHORIZATION, expiredS2sToken);
    }

    @Step("the request contains the path parameter {0} as {1}")
    public void theRequestContainsThePathParameter(String pathParam, String value) {
        request = request.pathParam(pathParam,value);
    }


    @Step("a call is submitted to the {0} endpoint using a {1} request")
    public void callIsSubmittedToTheEndpoint(String resource, String method) {
        Endpoints resourceAPI = Endpoints.valueOf(resource);

        switch (method.toUpperCase()) {
            case "POST" -> SerenityRest.when().post(resourceAPI.getResource());
            case "GET" -> SerenityRest.when().get(resourceAPI.getResource());
            case "DELETE" -> SerenityRest.when().delete(resourceAPI.getResource());
            default -> throw new IllegalStateException(TestConstants.UNEXPECTED_VALUE + method.toUpperCase());
        }
    }

    @Step("Check status code is {0}")
    public void checkStatusCode(int statusCode) {
        SerenityRest.then().assertThat().statusCode(statusCode);
    }

    @Step("the response body contains {0} as {1}")
    public void theResponseBodyContains(String attribute, String value) {
        SerenityRest.then().assertThat().body(attribute, Matchers.equalTo(value));
    }
}
