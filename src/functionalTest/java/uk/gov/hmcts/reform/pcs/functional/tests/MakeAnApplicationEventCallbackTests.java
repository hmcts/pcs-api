package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
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
import net.serenitybdd.rest.SerenityRest;

@Slf4j
@Tag("Functional_1")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MakeAnApplicationEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private final String caseType = CaseType.getCaseType();

    // Using static class memory so it persists cleanly across both tests
    private static String savedEventToken;

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreatedAndIssued("england");
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

        savedEventToken = SerenityRest.lastResponse().jsonPath().getString("token");
    }

    @Title("makeAnApplication submit event callback test - returns 200")
    @Test
    @Order(2)
    void makeAnApplicationSubmitEventCallbackTest() {
        DecodedJWT decodedJWT = JWT.decode(savedEventToken);
        String decodedCaseId = decodedJWT.getClaim("case-id").asString();

        String submitApplicationRequestBody = PayloadLoader.load(
            "/payloads/makeAnApplication-submitEventCallbackRequest.json",
            Map.of(
                "caseId", String.valueOf(caseReference),
                "internal_case_id", decodedCaseId,
                "caseTypeId", caseType,
                "currentRepresentedPartyId", "5659e6a9-8d00-45b5-a5c3-694e5a593cd3",
                "currentRepresentedPartyName", "Possession Claims Solicitor Org",
                "eventToken", savedEventToken
            )
        );

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.solicitorUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsIdempotencyKeyHeader();
        apiSteps.theRequestContainsTheQueryParameter("eventId", "makeAnApplication");
        apiSteps.theRequestContainsBody(submitApplicationRequestBody);

        apiSteps.callIsSubmittedToTheEndpoint("SubmitEventCallback", "POST");
        apiSteps.checkStatusCode(200);
    }
}
