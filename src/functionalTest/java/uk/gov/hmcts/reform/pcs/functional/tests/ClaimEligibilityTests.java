package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Issue;
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

//Although this is a testing support endpoint, we’re adding some functional tests to ensure the logic works as expected

@Issue("HDPI-1285")
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class ClaimEligibilityTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    @Title("Claim Eligibility endpoint - returns 200 and ELIGIBLE for an active whitelisted postcode")
    @Test
    void shouldReturnEligible() throws IOException {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "W3 7RX");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "src/functionalTest/resources/responses/claimEligibilityEligibleResponse.json");
    }

    @Title("Claim Eligibility endpoint - returns 200 and NOT_ELIGIBLE for an active postcode that is not whitelisted")
    @Test
    void shouldReturnNotEligible() throws IOException {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "W3 6RS");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "src/functionalTest/resources/responses/claimEligibilityNotEligibleResponse.json");
    }

    @Title("Claim Eligibility endpoint - returns 200 and LEGISLATIVE_COUNTRY_REQUIRED for cross-border postcode")
    @Test
    void shouldReturnLegislativeCountryRequired() throws IOException {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "SY132LH");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "src/functionalTest/resources/responses/claimEligibilityLegislativeCountryRequired.json");
    }

    @Title("Claim Eligibility endpoint - returns 200 and NO_MATCH_FOUND for expired postcode")
    @Test
    void shouldReturnNoMatchFound() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "RG61JS");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyContainsAString("status", "NO_MATCH_FOUND");
    }

    @Title("Claim Eligibility endpoint - returns 200 and MULTIPLE_MATCHES_FOUND for duplicated postcodes")
    @Test
    void shouldReturnMultipleMatchesFound() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "SW1 1AA");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyContainsAString("status", "MULTIPLE_MATCHES_FOUND");
    }

    // Jo's practice tests
    @Title("Claim Eligibility endpoint - returns 200 and NO_MATCH_FOUND for postcode that doesn't exist in db")
    @Test
    void shouldReturnNoMatchForNonExistentPostcode() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsTheQueryParameter("postcode", "L332AA");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyContainsAString("status", "NO_MATCH_FOUND");
    }

    @Title("Claim Eligibility endpoint - returns 400 for missing postcode parameter")
    @Test
    void shouldReturnBadRequest() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(400);
        apiSteps.theResponseBodyContainsAString("title", "Bad Request");
        apiSteps.theResponseBodyContainsAString("detail", "Required parameter 'postcode' is not present.");
    }

    @Title("Claim Eligibility endpoint - returns 401 for invalid auth header")
    @Test
    void shouldReturnUnauthorised() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsExpiredServiceToken();
        apiSteps.theRequestContainsTheQueryParameter("postcode", "L332AA");
        apiSteps.callIsSubmittedToTheEndpoint("ClaimEligibility", "GET");
        apiSteps.checkStatusCode(401);
    }
}
