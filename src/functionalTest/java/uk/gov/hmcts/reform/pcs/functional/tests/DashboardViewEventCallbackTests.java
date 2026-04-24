package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.CaseRoleCleanUp;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;

import java.util.Map;

@Slf4j
@Tag("Functional")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
public class DashboardViewEventCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String accessCode;

    private static final String caseType = CaseType.getCaseType();

    @BeforeEach
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
    }

    @AfterEach
    void cleanUp() {
        CaseRoleCleanUp.cleanUpCaseRole(caseReference.toString(), TestConstants.PCS_SOLICITOR_AUTOMATION_IDAM_UID,
                                        "[CREATOR]");
    }

    @Title("dashboardView start event callback test - returns 200")
    @Test
    void dashboardViewStartEventCallbackTest() {
        String dashboardViewRequestBody = PayloadLoader.load(
            "/payloads/dashboardView-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        String dashboardViewResponseBody = PayloadLoader.load(
            "/responses/dashboardView-startEventCallbackResponse.json",
            Map.of("caseId", caseReference)
        );

        apiSteps.validateAccessCode(caseReference.toString(), accessCode);
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsTheQueryParameter("eventId", "dashboardView");
        apiSteps.theRequestContainsBody(dashboardViewRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("StartEventCallback", "POST");
        apiSteps.checkStatusCode(200);
        apiSteps.theResponseBodyMatchesTheExpectedResponse(dashboardViewResponseBody);
    }
}
