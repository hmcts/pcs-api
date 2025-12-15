package uk.gov.hmcts.reform.pcs.functional.tests;

import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestCaseHelper;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class ValidateAccessCodeTests {

    private static final String baseUrl = System.getenv("TEST_URL");
    private final TestCaseHelper helper = new TestCaseHelper(baseUrl);

    private Long testCaseReference;
    private CreateTestCaseResponse testCase;

    @BeforeEach
    void setUp() {
        testCase = helper.createTestCaseWithDefendant();
        testCaseReference = testCase.getCaseReference();
    }

    @AfterEach
    void tearDown() {
        if (testCaseReference != null) {
            helper.deleteTestCase(testCaseReference);
            testCaseReference = null;
        }
    }

    @Title("Validate Access Code - should successfully link user with valid access code")
    @Test
    void shouldSuccessfullyLinkUserWithValidAccessCode() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        assertThat(accessCode).isNotNull();
        assertThat(accessCode).isNotEmpty();

        String citizenToken = helper.getCitizenIdamToken();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(200);

        // Verify linkage by attempting to link again - should return 409
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(409)
            .body("message", equalTo("This access code is already linked to a user."));
    }

    @Title("Validate Access Code - should return 400 when access code is invalid")
    @Test
    void shouldReturn400WhenAccessCodeIsInvalid() {
        Long caseReference = testCase.getCaseReference();
        String citizenToken = helper.getCitizenIdamToken();
        String invalidAccessCode = "INVALIDCODE123";
        Map<String, String> requestBody = Map.of("accessCode", invalidAccessCode);

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    @Title("Validate Access Code - should return 400 when case is not found")
    @Test
    void shouldReturn404WhenCaseNotFound() {
        Long nonExistentCaseReference = 999999999999L;
        Map<String, String> requestBody = Map.of("accessCode", "ANYCODE123");
        String citizenToken = helper.getCitizenIdamToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", nonExistentCaseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    @Title("Validate Access Code - should return 409 when access code is already used")
    @Test
    void shouldReturn409WhenAccessCodeAlreadyUsed() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        assertThat(accessCode).isNotNull();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        // Link User A
        String firstUserToken = helper.getCitizenIdamToken();
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + firstUserToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(200);

        // User B tries to use same access code
        String secondUserToken = helper.getCitizenIdamToken();
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + secondUserToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(409)
            .body("message", equalTo("This access code is already linked to a user."));
    }

    @Title("Validate Access Code - should return 409 when user is already linked to another defendant")
    @Test
    void shouldReturn409WhenUserAlreadyLinkedToAnotherDefendant() {
        CreateTestCaseResponse multiDefendantCase = helper.createTestCaseWithMultipleDefendants(2);
        Long caseReference = multiDefendantCase.getCaseReference();

        try {
            String firstAccessCode = multiDefendantCase.getDefendants().get(0).getAccessCode();
            String secondAccessCode = multiDefendantCase.getDefendants().get(1).getAccessCode();
            assertThat(firstAccessCode).isNotNull();
            assertThat(secondAccessCode).isNotNull();

            String idamToken = helper.getCitizenIdamToken();

            // Link to Defendant 1
            SerenityRest.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .header(TestConstants.AUTHORIZATION, "Bearer " + idamToken)
                .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", firstAccessCode))
                .when()
                .post("/cases/{caseReference}/validate-access-code")
                .then()
                .statusCode(200);

            // Same user tries to link to Defendant 2
            SerenityRest.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .header(TestConstants.AUTHORIZATION, "Bearer " + idamToken)
                .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", secondAccessCode))
                .when()
                .post("/cases/{caseReference}/validate-access-code")
                .then()
                .statusCode(409)
                .body("message", equalTo("This user ID is already linked to another party in this case."));
        } finally {
            helper.deleteTestCase(caseReference);
        }
    }

    @Title("Validate Access Code - should return 400 when Authorization header is missing")
    @Test
    void shouldReturn400WhenAuthorizationHeaderMissing() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    @Title("Validate Access Code - should return 401 when ServiceAuthorization header is missing")
    @Test
    void shouldReturn403WhenServiceAuthorizationHeaderMissing() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);
        String citizenToken = helper.getCitizenIdamToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(401);
    }

    @Title("Validate Access Code - should return 400 when access code is missing in request body")
    @Test
    void shouldReturn400WhenAccessCodeIsMissing() {
        Long caseReference = testCase.getCaseReference();
        Map<String, String> requestBody = Map.of();
        String citizenToken = helper.getCitizenIdamToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    @Title("Validate Access Code - should return 400 when access code is blank")
    @Test
    void shouldReturn400WhenAccessCodeIsBlank() {
        Long caseReference = testCase.getCaseReference();
        Map<String, String> requestBody = Map.of("accessCode", "");
        String citizenToken = helper.getCitizenIdamToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, helper.getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }
}
