package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.CaseRoleCleanUp;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;

import java.util.Map;

@Slf4j
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class PartyAccessCodeEndpointTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String accessCode;

    @BeforeEach
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
    }

    @AfterEach
    void cleanUp() {
        CaseRoleCleanUp.cleanUpCaseRole(caseReference.toString(), TestConstants.PCS_SOLICITOR_AUTOMATION_IDAM_UID,
                                        "[CREATOR]");
    }

    @Title("Party Access Code Endpoint Tests - should return 200 when successfully link user with valid access code")
    @Test
    void partyAccessCodeTest200Scenario() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(200);
    }

    @Title("Party Access Code Endpoint Tests - should return 400 when access code is invalid")
    @Test
    void partyAccessCodeTest400ScenarioInvalidAccessCode() {
        Map<String, String> requestBody = Map.of("accessCode", "INVALIDCODE123");

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(400);
        apiSteps.theResponseBodyContainsAString("message", "Invalid data");
    }

    @Title("Party Access Code Endpoint Tests - should return 400 when access code is empty")
    @Test
    void partyAccessCodeTest400ScenarioMissingAccessCode() {
        Map<String, String> requestBody = Map.of("accessCode", "");

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(400);
        apiSteps.theResponseBodyContainsAString("message", "Invalid data");
    }

    @Title("Party Access Code Endpoint Tests - should return 401 when S2S is missing")
    @Test
    void partyAccessCodeTest401MissingServiceToken() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(401);
    }

    @Title("Party Access Code Endpoint Tests - should return 401 when S2S is invalid")
    @Test
    void partyAccessCodeTest401InvalidServiceToken() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsExpiredServiceToken();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(401);
    }

    @Title("Party Access Code Endpoint Tests - return 403 Forbidden when the request uses an unauthorised S2S token")
    @Test
    void partyAccessCodeTest403Scenario() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsUnauthorisedServiceToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(403);
    }

    @Title("Party Access Code Endpoint Tests - should return a 409 when request is duplicated")
    @Test
    void partyAccessCodeTest409Scenario() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        //resend request to get 409 error.
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(409);
        apiSteps.theResponseBodyContainsAString("message", "This access code is already linked to a user.");

    }

    @Title("Party Access Code Endpoint Tests - Should return 404 when invalid case reference is used.")
    @Test
    void partyAccessCodeTest404Scenario() {
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_FRONTEND);
        apiSteps.theRequestContainsValidIdamToken(PcsIdamTokenClient.UserType.citizenUser);
        apiSteps.theRequestContainsThePathParameter("caseReference", "1234123412341234");
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(404);
        apiSteps.theResponseBodyContainsAString("message", "No case found with reference 1234123412341234");
    }
}
