package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;

import java.io.IOException;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class DashboardTasksEndpointTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    // Currently, endpoint use mock data and happy path tests only check that valid tokens return a 200 response.
    // Once real data is available, add assertions to validate response content to happy path scenarios.

    @Title("Dashboard tasks endpoint - return 200 when request is valid and uses pcs_api s2s token")
    @Test
    void dashboardTasks200SuccessWithPCSApiTokenScenario() throws IOException {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardTasks", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "src/functionalTest/resources/responses/dashboardTasksResponse.json");

    }

    @Title("Dashboard tasks endpoint - return 200 when request is valid and uses pcs_frontend s2s token")
    @Test
    void dashboardTasks200SuccessWithFrontendTokenScenario() throws IOException {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardTasks", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "src/functionalTest/resources/responses/dashboardTasksResponse.json");
    }

    @Title("Dashboard tasks endpoint - return 404 when case id doesn't exist")
    @Test
    void dashboardTasks404NotFoundScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", "9999");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardTasks", "GET");
        apiSteps.checkStatusCode(404);
        apiSteps.theResponseBodyContainsAString("message", "No case found with reference 9999");
    }

    @Title("Dashboard tasks endpoint - return 403 Forbidden when the request uses an unauthorised service token")
    @Test
    void dashboardTasks403ForbiddenScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsUnauthorisedServiceToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardTasks", "GET");
        apiSteps.checkStatusCode(403);
    }

    @Title("Dashboard tasks endpoint - return 401 Unauthorised when the request uses an invalid service token")
    @Test
    void dashboardTasks401UnauthorisedScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsExpiredServiceToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardTasks", "GET");
        apiSteps.checkStatusCode(401);
    }
}
