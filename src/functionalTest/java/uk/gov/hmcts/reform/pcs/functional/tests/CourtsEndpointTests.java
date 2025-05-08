package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Issue;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.annotations.Steps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class CourtsEndpointTests {

    @Steps
    static ApiSteps apiSteps;

    private final String postcodeValid = "W3 7RX";
    private final String postcodeInvalid = "W3 7RY";
    private final String expectedCourtName = "Central London County Court";
    private final int expectedCourtId = 40827;
    private final int expectedEpimId = 20262;


    @BeforeAll
    static void beforeAll() {
        apiSteps = new ApiSteps();
        apiSteps.setUp();
    }

    // Currently, postcode is hardcoded to check that API return a 200 response with Valid response which is also
    // hardcoded.Once real data is available, DB connections needs to be established to identify the postcode to
    // be used and to construct expected response Test is written with an assumption of single set of data in response,
    // need improvements when more than one court name can returned
    @Nested
    class SuccessScenarios {
        @Issue("HDPI-352")
        @Title("Courts endpoint - return 200 when request is valid and uses pcs_api s2s token & Idam Token")
        @Test
        void courts200SuccessWithPCSApiTokenandIdamTokenScenario() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - return 200 when request is valid and uses pcs_frontend s2s token & Idam Token")
        @Test
        void courts200SuccessWithPCSFrontendTokenandIdamTokenScenario() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_frontend");
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - returns 200 and expected court data for valid postcode")
        @Test
        void shouldReturnExpectedCourtForPostcode() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
            apiSteps.theResponseContains(expectedCourtName, expectedCourtId, expectedEpimId);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - returns 200 and empty list for postcode that doesn't exist")
        @Test
        void shouldReturnEmptyListForPostcodeNotExist() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeInvalid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
            apiSteps.theResponseContains(null, null, null);
        }
    }

    @Nested

    class UnauthorizedScenarios {

        @Issue("HDPI-352")
        @Title("Courts endpoint - return 403 Forbidden when the request uses an unauthorised s2s token")
        @Test
        void courts403ForbiddenScenario() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsUnauthorisedServiceToken();
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(403);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - return 401 Unauthorised when the request uses an invalid service token")
        @Test
        void courts401UnauthorisedScenarioInvalidServiceToken() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsExpiredServiceToken();
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(401);
        }

        //This test needs to be modified later to assert the response code as 401. As of now API response code is 200
        //for Invalid/Expired Idam Token
        @Issue("HDPI-352")
        @Title("Courts endpoint - return 401 Unauthorised when the request uses an invalid Idam token")
        @Test
        void courts401UnauthorisedScenarioInvalidIdamToken() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsExpiredIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - return 401 Unauthorised when the request uses an invalid service and Idam token")
        @Test
        void courts401UnauthorisedScenarioInvalidTokens() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsExpiredServiceToken();
            apiSteps.theRequestContainsExpiredIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(401);
        }

        @Issue("HDPI-352")
        @Title("Courts endpoint - returns 400 Bad Request for missing Idam Token")
        @Test
        void shouldReturn400ForMissingIdamToken() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsTheQueryParameter("postcode", postcodeValid);
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(400);
        }

        //This test needs to be modifed to check for response status 400, currently API returns 200
        @Issue("HDPI-352")
        @Title("Courts endpoint - returns 400 Bad Request for invalid postcode")
        @Test
        void shouldReturn400ForInvalidPostcode() {
            apiSteps.requestIsPreparedWithAppropriateValues();
            apiSteps.theRequestContainsValidServiceToken("pcs_api");
            apiSteps.theRequestContainsValidIdamToken();
            apiSteps.theRequestContainsTheQueryParameter("postcode", "");
            apiSteps.callIsSubmittedToTheEndpoint("Courts", "GET");
            apiSteps.checkStatusCode(200);
        }
    }
}
