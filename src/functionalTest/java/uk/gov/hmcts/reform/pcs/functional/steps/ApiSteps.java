package uk.gov.hmcts.reform.pcs.functional.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.testutils.IdamAuthenticationGenerator;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;

import java.util.List;
import java.util.Map;

public class ApiSteps {

    private RequestSpecification request;
    private Response response;

    private final String baseUrl = System.getenv("TEST_URL");
    private String pcsApiS2sToken;
    private String pcsFrontendS2sToken;
    private String unauthorisedS2sToken;
    private String idamToken;

    public void setUp() {

        ServiceAuthenticationGenerator serviceAuthenticationGenerator = new ServiceAuthenticationGenerator();
        pcsApiS2sToken = serviceAuthenticationGenerator.generate();
        pcsFrontendS2sToken = serviceAuthenticationGenerator.generate("pcs_frontend");
        unauthorisedS2sToken = serviceAuthenticationGenerator.generate("civil_service");
        idamToken = IdamAuthenticationGenerator.generateToken();

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
            "pcs_api", pcsApiS2sToken,
            "pcs_frontend", pcsFrontendS2sToken
        );

        if (!serviceTokens.containsKey(microservice.toLowerCase())) {
            throw new IllegalArgumentException("Unknown microservice: " + microservice);
        }

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
        response.then().assertThat().statusCode(statusCode);
    }

    @Step("the response body contains {string} as {string}")
    public void theResponseBodyContains(String attribute, String value) {
        response.then().assertThat().body(attribute, Matchers.equalTo(value));
    }

    @Step("the request contains a valid IDAM token")
    public void theRequestContainsValidIdamToken() {

        request = request.header("Authorization", "Bearer " + idamToken);
    }

    @Step("the request contains an expired IDAM token")
    public void theRequestContainsExpiredIdamToken() {
        String expiredIdamToken = "eyJ0eXAiOiJKV1QiLCJraWQiOiIxZXIwV1J3Z0lPVEFGb2pFNHJDL2ZiZUt1M0k9IiwiYWxnIjoiUlMyNTYi"
            + "fQ.eyJzdWIiOiJwY3Mtc3lzdGVtLXVwZGF0ZUBobWN0cy5uZXQiLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXV0aF9sZ"
            + "XZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiNzdmYzNjZjgtMTk2My00YTEzLWEwZGMtODZhNjg2MGEyNzU5LTMxMTUxMTQiLCJzdWJuY"
            + "W1lIjoicGNzLXN5c3RlbS11cGRhdGVAaG1jdHMubmV0IiwiaXNzIjoiaHR0cHM6Ly9mb3JnZXJvY2stYW0uc2VydmljZS5jb3JlLWNvb"
            + "XB1dGUtaWRhbS1hYXQyLmludGVybmFsOjg0NDMvb3BlbmFtL29hdXRoMi9yZWFsbXMvcm9vdC9yZWFsbXMvaG1jdHMiLCJ0b2tlbk5hb"
            + "WUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiJFNUhzQlpMeVlvLWEwYkQzb29EWG9GO"
            + "URGa2ciLCJhdWQiOiJjaXZpbF9jaXRpemVuX3VpIiwibmJmIjoxNzQ2MDQ3MjgwLCJncmFudF90eXBlIjoicGFzc3dvcmQiLCJzY29wZ"
            + "SI6WyJvcGVuaWQiLCJwcm9maWxlIiwicm9sZXMiXSwiYXV0aF90aW1lIjoxNzQ2MDQ3MjgwLCJyZWFsbSI6Ii9obWN0cyIsImV4cCI6M"
            + "Tc0NjA3NjA4MCwiaWF0IjoxNzQ2MDQ3MjgwLCJleHBpcmVzX2luIjoyODgwMCwianRpIjoicUFVQzk2SHBDbUR5aTdZLVB1bUpZUVNHe"
            + "VM0In0.jFcZmIgfELRJFRhuFk7d8ZIsbJeZA21lyVW-06HBQlXM-1yKAT8b52UsOojY1v7GyaTorj1i0CoFpDn9Ao0eLB6DCEETKE6HI"
            + "8Fq1PflA-mwBmR0F1nUGWHxPfWp4a2TaTkvhKiUxMkCCwfvTJLjBndJi3iCNezRzvxoHMJtKCOVR8PvNQu6pnxNha0xIC9DSBBFt0VjX"
            + "Kma_P3Pijxi_Ce69kZ3B6ZertwCy4ElmbPQhJgCjbQHimxSa-Omh-zcQG1MD1h1WPYN3OQLS4anvsNFWlDiBmaexN5C7Kru_LTTGLa5w"
            + "p604m_FAF2nmpgZdsP7o4MQ6Wd00DXlgY1fAw";
        request = request.header("Authorization", "Bearer " + expiredIdamToken);
    }

    @Step("the request contains the query parameter {0} as {1}")
    public void theRequestContainsTheQueryParameter(String queryParam, String value) {
        request = request.queryParam(queryParam, value);
    }

    @Step("validate response for court with optional name, id, and epimId")
    public void theResponseContains(String name, Integer id, Integer epimId) {
        List<Map<String, Object>> courts = response.jsonPath().getList("");

        if (name == null && id == null && epimId == null) {
            Assertions.assertTrue(courts.isEmpty(), "Expected empty response array, but got: " + courts);
        } else {
            // Ensure response is not empty
            Assertions.assertFalse(courts.isEmpty(), "Expected courts in the response but got an empty list.");

            // Try to find a matching court
            boolean matchFound = courts.stream().anyMatch(court -> {
                boolean nameMatches = name == null || name.equals(court.get("name"));
                boolean idMatches = id == null || id.equals(court.get("id"));
                boolean epimIdMatches = epimId == null || epimId.equals(court.get("epimId"));
                return nameMatches && idMatches && epimIdMatches;
            });

            Assertions.assertTrue(matchFound,
                                  String.format("Expected a court name='%s', id=%s, epimId=%s but none found in: %s",
                                                name, id, epimId, courts));
        }
    }

}
