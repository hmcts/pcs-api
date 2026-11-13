package uk.gov.hmcts.reform.pcs.functional.steps;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.JsonAssertUtils;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers.UserType;

public abstract class ApiSteps<T extends ApiSteps<T>> {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestSpecification request;
    protected Response response;
    protected String idamToken;
    private static boolean initialised;
    public static String pcsApiS2sToken;
    protected static String pcsFrontendS2sToken;
    protected static String ccdDataS2sToken;
    private static String unauthorisedS2sToken;

    @Step("Generate shared S2S tokens")
    public static synchronized void setUp() {
        if (initialised) {
            return;
        }
        ServiceAuthenticationGenerator generator = new ServiceAuthenticationGenerator();
        final String apiToken = generator.generate();
        final String frontendToken = generator.generate(TestConstants.PCS_FRONTEND);
        final String ccdToken = generator.generate("ccd_data");
        final String unauthorisedToken = generator.generate(TestConstants.CIVIL_SERVICE);

        pcsApiS2sToken = apiToken;
        pcsFrontendS2sToken = frontendToken;
        ccdDataS2sToken = ccdToken;
        unauthorisedS2sToken = unauthorisedToken;
        initialised = true;
    }

    protected abstract String baseUrl();

    protected abstract T self();

    @Step("with user {0}")
    public T withUser(UserType userType) {
        this.idamToken = TestUsers.getToken(userType);
        return self();
    }

    protected String getCurrentUserId() {
        if (idamToken == null || idamToken.isBlank()) {
            throw new IllegalStateException("Select a user before accessing their ID");
        }
        String userId = JWT.decode(idamToken).getSubject();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Selected user token has no subject");
        }
        return userId;
    }

    protected void resetHttpState() {
        request = null;
        response = null;
    }

    protected RequestSpecification newRequest() {
        resetHttpState();
        String url = baseUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("No base URL configured for " + getClass().getSimpleName());
        }
        request = SerenityRest.given().baseUri(url).contentType(ContentType.JSON);
        return request;
    }

    protected RequestSpecification getRequest() {
        if (request == null) {
            throw new IllegalStateException(
                "No request prepared. Prepare a request before adding values or submitting.");
        }
        return request;
    }

    public Response getResponse() {
        if (response == null) {
            throw new IllegalStateException("No response available. Submit a request first.");
        }
        return response;
    }

    @Step("a request is prepared with appropriate values")
    public void requestIsPreparedWithAppropriateValues() {
        newRequest();
    }

    @Step("the request contains header {0}")
    public void theRequestContainsHeader(String name, String value) {
        getRequest().header(name, value);
    }

    @Step("the request contains a valid service token for {0}")
    public void theRequestContainsValidServiceToken(String microservice) {
        String token = switch (microservice.toLowerCase(Locale.ROOT)) {
            case TestConstants.PCS_API -> pcsApiS2sToken;
            case TestConstants.PCS_FRONTEND -> pcsFrontendS2sToken;
            case "ccd_data" -> ccdDataS2sToken;
            default -> throw new IllegalArgumentException("Unknown microservice: " + microservice);
        };
        theRequestContainsHeader(TestConstants.SERVICE_AUTHORIZATION, token);
    }

    @Step("the request contains an unauthorised service token")
    public void theRequestContainsUnauthorisedServiceToken() {
        theRequestContainsHeader(TestConstants.SERVICE_AUTHORIZATION, unauthorisedS2sToken);
    }

    @Step("the request contains an expired service token")
    public void theRequestContainsExpiredServiceToken() {
        theRequestContainsHeader(TestConstants.SERVICE_AUTHORIZATION, TestConstants.EXPIRED_S2S_TOKEN);
    }

    @Step("the request contains an Idempotency-Key header")
    public void theRequestContainsIdempotencyKeyHeader() {
        theRequestContainsHeader("Idempotency-Key", UUID.randomUUID().toString());
    }

    @Step("the request contains the path parameter {0} as {1}")
    public void theRequestContainsThePathParameter(String pathParam, String value) {
        getRequest().pathParam(pathParam, value);
    }

    @Step("a call is submitted to the {0} endpoint using a {1} request")
    public void callIsSubmittedToTheEndpoint(String resource, String method) {
        response = null;
        // Serenity records the String overload, but not Rest Assured's Method overload.
        response = getRequest().when().request(Method.valueOf(method.toUpperCase(Locale.ROOT)).name(), resource);
    }

    @Step("Check status code is {0}")
    public void checkStatusCode(int statusCode) {
        getResponse().then().assertThat().statusCode(statusCode);
    }

    @Step("the response body contains {0} as a string: {1}")
    public void theResponseBodyContainsAString(String attribute, String value) {
        getResponse().then().assertThat().body(attribute, Matchers.equalTo(value));
    }

    @Step("the response body matches the expected list")
    public void theResponseBodyMatchesTheExpectedList(List<Map<String, Object>> expectedList) {
        getResponse().then().assertThat().body("", Matchers.equalTo(expectedList));
    }

    @Step("the response body is an empty array")
    public void theResponseBodyIsAnEmptyArray() {
        getResponse().then().assertThat().body("", Matchers.hasSize(0));
    }

    @Step("the response body matches the expected response")
    public void theResponseBodyMatchesTheExpectedResponse(String expectedPath) {
        JsonAssertUtils.assertEqualsIgnoreFields(expectedPath, getResponse().getBody().asString());
    }

    protected String idamTokenFor(UserType user) {
        return TestUsers.getToken(user);
    }

    @Step("the request contains a valid IDAM token")
    public void theRequestContainsValidIdamToken(UserType user) {
        theRequestContainsHeader(TestConstants.AUTHORIZATION, "Bearer " + idamTokenFor(user));
    }

    @Step("the request contains an expired IDAM token")
    public void theRequestContainsExpiredIdamToken() {
        theRequestContainsHeader(TestConstants.AUTHORIZATION, "Bearer " + TestConstants.EXPIRED_IDAM_TOKEN);
    }

    @Step("the request contains the query parameter {0} as {1}")
    public void theRequestContainsTheQueryParameter(String queryParam, String value) {
        getRequest().queryParam(queryParam, value);
    }

    @Step("the request contains a request body")
    public void theRequestContainsBody(Object body) {
        getRequest().body(body);
    }
}
