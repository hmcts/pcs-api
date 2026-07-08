package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.Objects;
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
    private String extractedPartyId;
    private String extractedPartyName;
    private String eventToken;
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

        // NATIVE FIX 1: Extract the payload token token natively via standard RestAssured options mapping
        eventToken = net.serenitybdd.rest.SerenityRest.lastResponse().jsonPath().getString("token");

        apiSteps.theResponseBodyMatchesTheExpectedResponse(
            "/responses/makeAnApplication-startEventCallbackResponse.json");

        extractedPartyId = net.serenitybdd.rest.SerenityRest.lastResponse().jsonPath().getString("data.currentRepresentedPartyId");
        if (extractedPartyId == null) {
            extractedPartyId = net.serenitybdd.rest.SerenityRest.lastResponse().jsonPath().getString("data.representedPartyNames.list_items[0].code");
        }

        extractedPartyName = net.serenitybdd.rest.SerenityRest.lastResponse().jsonPath().getString("data.currentRepresentedPartyName");
        if (extractedPartyName == null) {
            extractedPartyName = net.serenitybdd.rest.SerenityRest.lastResponse().jsonPath().getString("data.representedPartyNames.list_items[0].label");
        }
    }

    @Title("makeAnApplication submit event callback test - returns 200")
    @Test
    @Order(2)
    void makeAnApplicationSubmitEventCallbackTest() {
        String submitApplicationRequestBody = PayloadLoader.load(
            "/payloads/makeAnApplication-submitEventCallbackRequest.json",
            Map.of(
                "currentRepresentedPartyId", Objects.toString(extractedPartyId, "2269a743-b1ea-4c2b-8c1e-b15caca1b120"),
                "currentRepresentedPartyName", Objects.toString(extractedPartyName, "Jane Doe"),
                "eventToken", Objects.toString(eventToken, "")
            )
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsIdempotencyKeyHeader();

        apiSteps.theRequestContainsThePathParameter("caseId", caseReference.toString());

        apiSteps.theRequestContainsBody(submitApplicationRequestBody);

        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
    }
}
