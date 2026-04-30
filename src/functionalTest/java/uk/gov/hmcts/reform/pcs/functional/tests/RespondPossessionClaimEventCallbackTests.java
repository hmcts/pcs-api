package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.CaseRoleCleanUp;

@Slf4j
@Tag("Functional")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RespondPossessionClaimEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String accessCode;

    private static final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
    }

    @AfterAll
    void cleanUp() {
        if (caseReference != null) {
            CaseRoleCleanUp.cleanUpCaseRole(
                caseReference.toString(),
                TestConstants.PCS_SOLICITOR_AUTOMATION_IDAM_UID,
                "[CLAIMANTSOLICITOR]"
            );
        }
    }

    @Title("respondToPossessionClaim start event callback test without access code - returns 403")
    @Test
    @Order(1)
    void respondToPossessionClaimStartEventCallbackWithoutAccessCodeAuthTest() {
        String respondClaimRequestBody = PayloadLoader.load(
            "/payloads/repondPossessionClaim-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "respondPossessionClaim");
        apiSteps.theRequestContainsBody(respondClaimRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(403);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/respondPossessionClaim-UnAuthorisedStartEventCallbackResponse.json");
    }

    @Title("respondToPossessionClaim start event callback test - returns 200")
    @Test
    @Order(2)
    void respondToPossessionClaimStartEventCallbackTest() {
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        String respondClaimRequestBody = PayloadLoader.load(
            "/payloads/repondPossessionClaim-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );
        apiSteps.validateAccessCode(caseReference.toString(), accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "respondPossessionClaim");
        apiSteps.theRequestContainsBody(respondClaimRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/respondPossessionClaim-startEventCallbackResponse.json");
    }

    @Title("respondToPossessionClaim submit event callback test - returns 200")
    @Test
    @Disabled("This feature is currently in development and evolving, disabled to avoid false negatives.")
    @Order(3)
    void respondToPossessionClaimSubmitEventCallbackTest() {
        String respondClaimRequestBody = PayloadLoader.load(
            "/payloads/repondPossessionClaim-submitEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "respondPossessionClaim");
        apiSteps.theRequestContainsBody(respondClaimRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/respondPossessionClaim-submitEventCallbackResponse.json");
    }
}
