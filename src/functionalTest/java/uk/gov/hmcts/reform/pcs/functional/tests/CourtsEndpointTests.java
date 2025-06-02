package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Issue;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.annotations.Steps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.CourtConstants;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;

@Issue("HDPI-352")
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class CourtsEndpointTests {

    @Steps
    ApiSteps apiSteps;

    @BeforeEach
    void beforeEach() {
        apiSteps.setUp();
    }

    // Currently, postcode is hardcoded to check that API return a 200 response with Valid response which is also
    // hardcoded. Once real data is available, DB connections needs to be established to identify the postcode to
    // be used and to construct expected response Test is written with an assumption of single set of data in response,
    // need improvements when more than one court name can be returned
    @Title("Courts endpoint - returns 200 and expected court data for valid postcode")
    @Test
    void shouldReturnExpectedCourtForPostcode() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedList(CourtConstants.EXPECTED_COURT_LIST);
    }

    @Title("Courts endpoint - returns 200 and empty list for postcode that doesn't exist in the database "
        + "and uses pcs_frontend S2S token")
    @Test
    void shouldReturnEmptyListForPostcodeNotExist() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_INVALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyIsAnEmptyArray();
    }

    @Title("Courts endpoint - returns 200 and expected court data for partial postcode")
    @Test
    void shouldReturnExpectedCourtForPartialPostcode() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.PARTIAL_POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedList(CourtConstants.EXPECTED_COURT_LIST);
    }

    @Title("Courts endpoint - return 403 Forbidden when the request uses an unauthorised S2S token")
    @Test
    void courts403ForbiddenScenario() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsUnauthorisedServiceToken();
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(403);
    }

    @Title("Courts endpoint - return 401 Unauthorised when the request uses an expired service token")
    @Test
    void courts401UnauthorisedScenarioExpiredServiceToken() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsExpiredServiceToken();
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(401);
    }

    @Title("Courts endpoint - return 401 Unauthorised when the request uses an expired Idam token")
    @Test
    void courts401UnauthorisedScenarioExpiredIdamToken() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken("pcs_api");
        apiSteps.theRequestContainsExpiredIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", CourtConstants.POSTCODE_VALID);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(401);
    }

    @Title("Courts endpoint - returns 400 Bad Request for missing postcode")
    @Test
    void shouldReturn400ForInvalidPostcode() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken("pcs_api");
        apiSteps.theRequestContainsValidIdamToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", null);
        apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
        apiSteps.checkStatusCode(400);
    }
}
