package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Issue;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;

import java.io.IOException;

//Although this is a testing support endpoint, we’re adding some functional tests to ensure the logic works as expected

@Issue("HDPI-1285")
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class ClaimEligibilityTests {

    @Steps
    ApiSteps apiSteps;

    @BeforeEach
    void beforeEach() {
        apiSteps.setUp();
    }

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
}
