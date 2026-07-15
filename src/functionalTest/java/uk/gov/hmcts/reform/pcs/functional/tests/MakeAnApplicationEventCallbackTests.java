package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.restassured.RestAssured;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import net.serenitybdd.rest.SerenityRest;

@Slf4j
@Tag("Functional_1")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MakeAnApplicationEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String representedPartyId;
    private final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");

        String accessCode = apiSteps.accessCodeIsFetched(caseReference);
        apiSteps.validateAccessCode(caseReference.toString(), accessCode);
    }

    @Title("makeAnApplication start event callback test - returns 200")
    @Test
    @Order(1)
    void makeAnApplicationStartEventCallbackTest() {
        String makeApplicationRequestBody = PayloadLoader.load(
            "/payloads/makeAnApplication-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(makeApplicationRequestBody);

        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);

        representedPartyId = SerenityRest.lastResponse().path("data.currentRepresentedPartyId");
        System.out.println("==================================================");
        System.out.println("[DEBUG] Captured Party ID: " + representedPartyId);
        System.out.println("==================================================");
    }

    @Title("makeAnApplication submit event callback test - returns 200")
    @Test
    @Order(2)
    void makeAnApplicationSubmitEventCallbackTest() {

        String liveCaseNoteToken = RestAssured.given()
            .baseUri("https://ccd-data-store-api-pcs-api-pr-2008.preview.platform.hmcts.net")
            .header(TestConstants.AUTHORIZATION, "Bearer " + ApiSteps.solicitorUserIdamToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, ApiSteps.pcsApiS2sToken)
            .header("Experimental", "True")
            .get("/cases/" + caseReference + "/event-triggers/addCaseNote")
            .then()
            .statusCode(200)
            .extract()
            .path("token");

        DecodedJWT decodedJWT = JWT.decode(liveCaseNoteToken);
        String decodedCaseId = decodedJWT.getClaim("case-id").asString();

        String submitApplicationRequestBody = PayloadLoader.load(
            "/payloads/makeAnApplication-submitEventCallbackRequest.json",
            Map.of(
                "caseId", String.valueOf(caseReference),
                "internalCaseId", decodedCaseId,
                "caseTypeId", caseType,
                "representedPartyId", representedPartyId
            )
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(submitApplicationRequestBody);

        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
    }
}
