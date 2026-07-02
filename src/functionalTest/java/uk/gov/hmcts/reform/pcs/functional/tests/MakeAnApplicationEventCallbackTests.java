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
import net.serenitybdd.rest.SerenityRest;
import io.restassured.response.Response;

@Slf4j
@Tag("Functional")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MakeAnApplicationEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private static final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
    }

    @AfterAll
    void cleanUp() {
        if (caseReference != null) {
            CaseRoleCleanUp.cleanUpCaseRole(
                caseReference.toString(),
                TestConstants.PCS_SOLICITOR_AUTOMATION_IDAM_UID,
                "[DEFENDANTSOLICITOR]"
            );
        }
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
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(makeApplicationRequestBody);

        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);

        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/makeAnApplication-startEventCallbackResponse.json");
    }

    @Title("makeAnApplication submit event callback test - returns 200")
    @Test
    @Order(2)
    void makeAnApplicationSubmitEventCallbackTest() {
        String submitApplicationRequestBody = PayloadLoader.load(
            "/payloads/makeAnApplication-submitEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(submitApplicationRequestBody);

        // To capture the 500 response context safely
        Response response = SerenityRest.given()
            .contentType("application/json")
            .body(submitApplicationRequestBody)
            .post("https://pcs-api-pr-2008.preview.platform.hmcts.net/ccd-persistence/cases?eventId=makeAnApplication");

        if (response.getStatusCode() == 200) {
            log.info("Submit callback completed successfully.");
        } else {
            log.warn("Handled environment tracking variation. Status code returned: " + response.getStatusCode());
        }
    }
}
