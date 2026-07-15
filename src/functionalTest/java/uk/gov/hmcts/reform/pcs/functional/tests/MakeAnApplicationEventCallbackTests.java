package uk.gov.hmcts.reform.pcs.functional.tests;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;

import java.util.Map;

import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.*;
import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

@Tag("Functional_1")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MakeAnApplicationEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String solicitor2Token;
    private String representedPartyId;
    private final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");

        //auth for sol2
        solicitor2Token = RestAssured.given()
            .baseUri(getEnv("IDAM_API_URL"))
            .contentType("application/x-www-form-urlencoded")
            .formParams(Map.of(
                "username", "pcs-solicitor2@test.com",
                "password", "Pa$$w0rd",
                "client_id", CLIENT_ID,
                "client_secret", getEnv("PCS_API_IDAM_SECRET"),
                "scope", SCOPE,
                "grant_type", GRANT_TYPE
            ))
            .post(ENDPOINT)
            .then().statusCode(200).extract().path("access_token");

        //Get party ID for def
        String partyId = RestAssured.given()
            .header("Authorization", "Bearer " + solicitorUserIdamToken)
            .header("ServiceAuthorization", pcsApiS2sToken)
            .get("https://pcs-api-pr-2008.preview.platform.hmcts.net/ccd-persistence/cases?case-refs=" + caseReference)
            .then().statusCode(200)
            .extract().path("case_data.allDefendants[0].id");

        //Link sol2
        RestAssured.given()
            .header("Authorization", "Bearer " + solicitor2Token)
            .header("ServiceAuthorization", pcsApiS2sToken)
            .post("https://pcs-api-pr-2008.preview.platform.hmcts.net/testing-support/link-defendant-solicitor-to-party/"
                      + caseReference + "/" + partyId);
    }

    @Test
    @Order(1)
    @Title("Start Callback - Returns 200")
    void makeAnApplicationStartEventCallbackTest() {
        String request = PayloadLoader.load("/payloads/makeAnApplication-startEventCallbackRequest.json",
                                            Map.of("caseTypeId", caseType, "caseId", caseReference));

        apiSteps.requestIsPreparedWithAppropriateValues();
        RestAssured.given().header("Authorization", "Bearer " + solicitor2Token);

        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(request);

        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);

        representedPartyId = RestAssured.lastResponse().path("data.currentRepresentedPartyId");
    }

    @Test
    @Order(2)
    @Title("Submit Callback - Returns 200")
    void makeAnApplicationSubmitEventCallbackTest() {

        String token = RestAssured.given()
            .header("Authorization", "Bearer " + solicitor2Token)
            .header("ServiceAuthorization", pcsApiS2sToken)
            .get("https://ccd-data-store-api-pcs-api-pr-2008.preview.platform.hmcts.net/cases/" + caseReference + "/event-triggers/addCaseNote")
            .then().statusCode(200).extract().path("token");

        String internalCaseId = JWT.decode(token).getClaim("case-id").asString();

        String request = PayloadLoader.load("/payloads/makeAnApplication-submitEventCallbackRequest.json",
                                            Map.of("caseId", caseReference,
                                                   "internalCaseId", internalCaseId,
                                                   "caseTypeId", caseType,
                                                   "representedPartyId", representedPartyId));

        apiSteps.requestIsPreparedWithAppropriateValues();
        RestAssured.given().header("Authorization", "Bearer " + solicitor2Token);

        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(request);

        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
    }
}
