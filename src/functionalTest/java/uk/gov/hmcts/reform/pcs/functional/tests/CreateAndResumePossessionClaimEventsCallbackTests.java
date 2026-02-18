package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.RandomNumberUtil;

import java.util.Map;

@Tag("Functional1")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateAndResumePossessionClaimEventsCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private static final Long caseId = RandomNumberUtil.generateRandomNumber(16);
    private static final String caseType = CaseType.getCaseType();

    @Title("createPossessionClaim start event callback test - returns 200")
    @Order(1)
    @Test
    void createPossessionClaimStartEventCallbackTest() {
        String requestBody = PayloadLoader.load(
            "/payloads/createPossessionClaim-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "createPossessionClaim");
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/createPossessionClaim-startEventCallbackResponse.json");
    }

    @Title("createPossessionClaim submit event callback test - returns 200")
    @Order(2)
    @Test
    void createPossessionClaimSubmitEventCallbackTest() {
        String requestBody = PayloadLoader.load(
            "/payloads/createPossessionClaim-submitEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseId)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "createPossessionClaim");
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/createPossessionClaim-submitEventCallbackResponse.json");
    }

    @Title("resumePossessionClaim start event callback test - returns 200")
    @Order(3)
    @Test
    void resumePossessionClaimStartEventCallbackTest() {
        String requestBody = PayloadLoader.load(
            "/payloads/resumePossessionClaim-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseId)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "resumePossessionClaim");
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/resumePossessionClaim-startEventCallbackResponse.json");
    }

    @Title("resumePossessionClaim submit event callback test - returns 200")
    @Order(4)
    @Test
    void resumePossessionClaimSubmitEventCallbackTest() {
        String requestBody = PayloadLoader.load(
            "/payloads/resumePossessionClaim-submitEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseId)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "resumePossessionClaim");
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/resumePossessionClaim-submitEventCallbackResponse.json");
    }
}
