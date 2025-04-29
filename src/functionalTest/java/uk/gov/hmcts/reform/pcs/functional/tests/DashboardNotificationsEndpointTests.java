package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.annotations.Steps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class DashboardNotificationsEndpointTests {

    @Steps
    ApiSteps apiSteps;

    @BeforeEach
    void beforeEach() {
        apiSteps.setUp();
    }

    // Currently, endpoint use mock data and happy path tests only check that valid tokens return a 200 response.
    // Once real data is available, add assertions to validate response content to happy path scenarios.

    @Title("Dashboard notifications endpoint - return 200 when request is valid and uses pcs_api s2s token")
    @Test
    void dashboardNotifications200SuccessWithPCSApiTokenScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardNotifications", "GET");
        apiSteps.checkStatusCode(200);
    }

    @Title("Dashboard notifications endpoint - return 200 when request is valid and uses pcs_frontend s2s token")
    @Test
    void dashboardNotifications200SuccessWithFrontendTokenScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardNotifications", "GET");
        apiSteps.checkStatusCode(200);
    }

    @Title("Dashboard notifications endpoint - return 404 when case id doesn't exist")
    @Test
    void dashboardNotifications404NotFoundScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", "9999");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardNotifications", "GET");
        apiSteps.checkStatusCode(404);
        apiSteps.theResponseBodyContains("message", "No case found with reference 9999");
    }

    @Title("Dashboard notifications endpoint - return 403 Forbidden when the request uses an unauthorised s2s token")
    @Test
    void dashboardNotifications403ForbiddenScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsUnauthorisedServiceToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardNotifications", "GET");
        apiSteps.checkStatusCode(403);
    }

    @Title("Dashboard notifications endpoint - return 401 Unauthorised when the request uses an invalid service token")
    @Test
    void dashboardNotifications401UnauthorisedScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsExpiredServiceToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", "1666630757927238");
        apiSteps.callIsSubmittedToTheEndpoint("DashboardNotifications", "GET");
        apiSteps.checkStatusCode(401);
    }
}
