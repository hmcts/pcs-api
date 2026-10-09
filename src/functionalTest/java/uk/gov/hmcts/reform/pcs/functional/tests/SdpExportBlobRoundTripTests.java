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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@Issue("HDPI-9102")
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class SdpExportBlobRoundTripTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    @Title("SDP export round trip - writes and reads back a blob in the SDP export container")
    @Test
    void shouldRoundTripABlobThroughTheSdpExportContainer() {
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.callIsSubmittedToTheEndpoint("SdpExportRoundTrip", "POST");
        // Checked first so a failure prints the Azure error rather than just the status code
        apiSteps.theResponseBodyAttributeMatches("error", nullValue());
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyAttributeMatches("roundTrip", equalTo(true));
        apiSteps.theResponseBodyAttributeMatches(
            "blobName", allOf(startsWith("functional-test/"), endsWith(".json")));
    }
}
