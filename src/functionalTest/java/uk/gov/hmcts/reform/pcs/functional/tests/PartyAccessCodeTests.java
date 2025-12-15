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
import uk.gov.hmcts.reform.pcs.functional.testutils.TestCaseGenerator;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class PartyAccessCodeTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private final TestCaseGenerator caseGenerator = new TestCaseGenerator();
    private CreateTestCaseResponse testCase;
    private Long testCaseReference;

    @BeforeEach
    void setUp() {
        // Create test data before each test
        testCase = caseGenerator.createTestCaseWithDefendant();
        testCaseReference = testCase.getCaseReference();
    }

    @AfterEach
    void tearDown() {
        if (testCaseReference != null) {
            caseGenerator.deleteTestCase(testCaseReference);
            testCaseReference = null;
        }
    }

    @Title("Party Access Code Tests - Access Code should be generated upon Case Creation")
    @Test
    void successfullyGenerateAccessCode() {
        CreateTestCaseResponse multiDefendantTestCase = caseGenerator.createTestCaseWithMultipleDefendants(2);

        String defendant1AccessCode = multiDefendantTestCase.getDefendants().get(0).getAccessCode();
        String defendant2AccessCode = multiDefendantTestCase.getDefendants().get(1).getAccessCode();

        assertThat(defendant1AccessCode).isNotNull();
        assertThat(defendant1AccessCode).isNotEmpty();

        assertThat(defendant2AccessCode).isNotNull();
        assertThat(defendant2AccessCode).isNotEmpty();
    }

    @Title("Party Access Code Tests - should successfully link user with valid access code")
    @Test
    void successfullyLinkUserWithValidAccessCode() {

        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(200);
    }

    @Title("Validate Access Code - should return 400 when access code is invalid")
    @Test
    void return400WhenAccessCodeIsInvalid() {

        String caseReference = testCase.getCaseReference().toString();
        Map<String, String> requestBody = Map.of("accessCode", "INVALIDCODE123");

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference);
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(400);
        apiSteps.theResponseBodyContainsAString("title", "Bad Request");
        apiSteps.theResponseBodyContainsAString("detail", "Invalid request content.");
    }

    @Title("Party Access Code Tests - should return 401 when ServiceAuthorization header is invalid/missing")
    @Test
    void return401WhenInvalidServiceAuthorizationToken() {
        String caseReference = testCase.getCaseReference().toString();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference);
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(401);
    }

    @Title("Party Access Code Tests - should return a 409 when request is duplicated")
    @Test
    void return409WhenDuplicateRequest() {

        String caseReference = testCase.getCaseReference().toString();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        // Initial link between case and access code
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference);
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(200);

        // Retry link to show that valid access codes are only single use
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", caseReference);
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(409);
        apiSteps.theResponseBodyContainsAString("message", "This access code is already linked to a user.");

    }

    @Title("Party Access Code Tests - Should return 404 when invalid case reference is used.")
    @Test
    void return404WhenCaseReferenceIsInvalid() {

        Long invalidCaseReference = 9999L;
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsValidCitizenIdamToken();
        apiSteps.theRequestContainsThePathParameter("caseReference", invalidCaseReference.toString());
        apiSteps.theRequestContainsBody(requestBody);
        apiSteps.callIsSubmittedToTheEndpoint("ValidateAccessCode", "POST");
        apiSteps.checkStatusCode(404);
        apiSteps.theResponseBodyContainsAString("message", "No case found with reference " + invalidCaseReference);
    }
}
