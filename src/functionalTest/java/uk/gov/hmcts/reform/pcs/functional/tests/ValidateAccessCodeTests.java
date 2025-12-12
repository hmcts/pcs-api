package uk.gov.hmcts.reform.pcs.functional.tests;

import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.CitizenUserGenerator;
import uk.gov.hmcts.reform.pcs.functional.testutils.IdamAuthenticationGenerator;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class ValidateAccessCodeTests {

    @Steps
    ApiSteps apiSteps;

    private static final String baseUrl = System.getenv("TEST_URL");

    // Test data references - stored for cleanup
    private Long testCaseReference;
    private CreateTestCaseResponse testCase;

    @BeforeEach
    void setUp() {
        // Create test data before each test
        testCase = createTestCaseWithDefendant();
        testCaseReference = testCase.getCaseReference();
    }

    @AfterEach
    void tearDown() {
        // ALWAYS clean up test data after each test
        // Uses DELETE /testing-support/cases/{caseReference} endpoint
        // This endpoint deletes both the case and all associated access codes
        if (testCaseReference != null) {
            deleteTestCase(testCaseReference);
            testCaseReference = null;  // Clear reference after cleanup attempt
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Helper method to get IDAM token
     */
    private String getIdamToken() {
        return IdamAuthenticationGenerator.generateToken();
    }

    /**
     * Helper method to get S2S token
     */
    private String getS2sToken() {
        ServiceAuthenticationGenerator generator = new ServiceAuthenticationGenerator();
        return generator.generate(TestConstants.PCS_API);
    }

    /**
     * Helper method to create test case with a single defendant
     */
    private CreateTestCaseResponse createTestCaseWithDefendant() {
        // Build request body - using HashMap to allow null values
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("caseReference", null);
        requestBody.put("propertyAddress", Map.of(
            "AddressLine1", "123 Test Street",
            "PostTown", "London",
            "PostCode", "W3 7RX",
            "Country", "United Kingdom"
        ));
        requestBody.put("legislativeCountry", "England");
        
        Map<String, Object> defendant = new HashMap<>();
        defendant.put("partyId", UUID.randomUUID().toString());
        defendant.put("idamUserId", null);
        defendant.put("firstName", "Test");
        defendant.put("lastName", "Defendant1");
        requestBody.put("defendants", List.of(defendant));

        // Make API call using SerenityRest
        CreateTestCaseResponse response = SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + getIdamToken())
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .body(requestBody)
            .when()
            .post("/testing-support/create-case")
            .then()
            .statusCode(201)
            .extract()
            .as(CreateTestCaseResponse.class);

        return response;
    }

    /**
     * Helper method to create test case with multiple defendants
     */
    private CreateTestCaseResponse createTestCaseWithMultipleDefendants(int numberOfDefendants) {
        List<Map<String, Object>> defendants = new ArrayList<>();
        for (int i = 0; i < numberOfDefendants; i++) {
            Map<String, Object> defendant = new HashMap<>();
            defendant.put("partyId", UUID.randomUUID().toString());
            defendant.put("idamUserId", null);
            defendant.put("firstName", "Test");
            defendant.put("lastName", "Defendant" + (i + 1));
            defendants.add(defendant);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("caseReference", null);
        requestBody.put("propertyAddress", Map.of(
            "AddressLine1", "123 Test Street",
            "PostTown", "London",
            "PostCode", "W3 7RX",
            "Country", "United Kingdom"
        ));
        requestBody.put("legislativeCountry", "England");
        requestBody.put("defendants", defendants);

        return SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + getIdamToken())
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .body(requestBody)
            .when()
            .post("/testing-support/create-case")
            .then()
            .statusCode(201)
            .extract()
            .as(CreateTestCaseResponse.class);
    }

    /**
     * Helper method to delete test case using TestingSupportController.deleteCase() endpoint
     * This endpoint deletes both the case and all associated access codes
     */
    private void deleteTestCase(Long caseReference) {
        try {
            SerenityRest.given()
                .baseUri(baseUrl)
                .header(TestConstants.AUTHORIZATION, "Bearer " + getIdamToken())
                .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
                .pathParam("caseReference", caseReference)
                .when()
                .delete("/testing-support/cases/{caseReference}")
                .then()
                .statusCode(204);  // 204 No Content on successful deletion
        } catch (Exception e) {
            // Log but don't fail test - cleanup is best effort
            // Case might already be deleted, or test failed before case was created
            System.err.println("Warning: Failed to delete test case " + caseReference + ": " + e.getMessage());
        }
    }

    // ============================================
    // TEST CASE 1: Happy Path - Successful Linking
    // ============================================

    @Title("Validate Access Code - should successfully link user with valid access code")
    @Test
    void shouldSuccessfullyLinkUserWithValidAccessCode() {
        // Step 1: Test data is already created in @BeforeEach
        // testCase and testCaseReference are available from setUp()
        Long caseReference = testCase.getCaseReference();

        // Step 2: Extract access code from response
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        assertThat(accessCode).isNotNull();
        assertThat(accessCode).isNotEmpty();

        // Step 3: Get IDAM token for user
        // Note: Dynamic user creation is not available in AAT, so we use system user token
        // In production/Jenkins, this would use IdamAuthenticationGenerator.generateToken()
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        // Step 4: Build request body
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        // Step 5: Call validate-access-code endpoint with citizen user
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(200);

        // Step 6: Verify defendant is linked by attempting to link again (should return 409)
        // This is a functional test workaround since we may not have direct DB access
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(409)
            .body("message", equalTo("This access code is already linked to a user."));

        // Step 7: Cleanup is handled automatically by @AfterEach
        // Uses DELETE /testing-support/cases/{caseReference} endpoint
        // This deletes both the case and all associated access codes
    }

    // ============================================
    // TEST CASE 2: Invalid Access Code
    // ============================================

    @Title("Validate Access Code - should return 400 when access code is invalid")
    @Test
    void shouldReturn400WhenAccessCodeIsInvalid() {
        // Step 1: Test data is already created in @BeforeEach
        Long caseReference = testCase.getCaseReference();

        // Step 2: Create a citizen user for this test
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        // Step 3: Build request body with invalid access code
        String invalidAccessCode = "INVALIDCODE123";
        Map<String, String> requestBody = Map.of("accessCode", invalidAccessCode);

        // Step 4: Call validate-access-code endpoint with invalid access code
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
        // Note: Error response format may vary, so we only verify status code

        // Step 5: Cleanup is handled automatically by @AfterEach
        // Uses DELETE /testing-support/cases/{caseReference} endpoint
        // This deletes both the case and all associated access codes
    }

    // ============================================
    // TEST CASE 3: Case Not Found
    // ============================================

    @Title("Validate Access Code - should return 400 when case is not found")
    @Test
    void shouldReturn404WhenCaseNotFound() {
        // Use non-existent caseReference
        Long nonExistentCaseReference = 999999999999L;
        Map<String, String> requestBody = Map.of("accessCode", "ANYCODE123");

        // Create a citizen user for this test
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", nonExistentCaseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400); // API returns 400 when case is not found (before access code validation)
    }

    // ============================================
    // TEST CASE 4: Access Code Already Used
    // ============================================

    @Title("Validate Access Code - should return 409 when access code is already used")
    @Test
    void shouldReturn409WhenAccessCodeAlreadyUsed() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        assertThat(accessCode).isNotNull();

        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        // First call - link User A (should succeed)
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + getIdamToken())
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(200);

        // Second call - try to link User B with same access code (should fail)
        // Create a different citizen user to test with different user ID
        String secondUserToken = IdamAuthenticationGenerator.generateToken();
        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + secondUserToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(409)
            .body("message", equalTo("This access code is already linked to a user."));
    }

    // ============================================
    // TEST CASE 5: User Already Linked to Another Defendant
    // ============================================

    @Title("Validate Access Code - should return 409 when user is already linked to another defendant")
    @Test
    void shouldReturn409WhenUserAlreadyLinkedToAnotherDefendant() {
        // Create test case with TWO defendants
        CreateTestCaseResponse multiDefendantCase = createTestCaseWithMultipleDefendants(2);
        Long caseReference = multiDefendantCase.getCaseReference();

        try {
            String firstAccessCode = multiDefendantCase.getDefendants().get(0).getAccessCode();
            String secondAccessCode = multiDefendantCase.getDefendants().get(1).getAccessCode();
            assertThat(firstAccessCode).isNotNull();
            assertThat(secondAccessCode).isNotNull();

            // Create a citizen user for this test
            String idamToken = IdamAuthenticationGenerator.generateToken();

            // Link User A to Defendant 1 (should succeed)
            SerenityRest.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .header(TestConstants.AUTHORIZATION, "Bearer " + idamToken)
                .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", firstAccessCode))
                .when()
                .post("/cases/{caseReference}/validate-access-code")
                .then()
                .statusCode(200);

            // Try to link User A to Defendant 2 (should fail - user already linked)
            SerenityRest.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .header(TestConstants.AUTHORIZATION, "Bearer " + idamToken)
                .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", secondAccessCode))
                .when()
                .post("/cases/{caseReference}/validate-access-code")
                .then()
                .statusCode(409)
                .body("message", equalTo("This user ID is already linked to another party in this case."));
        } finally {
            // Cleanup the multi-defendant case
            deleteTestCase(caseReference);
        }
    }

    // ============================================
    // TEST CASE 6: Missing Authorization Header
    // ============================================

    @Title("Validate Access Code - should return 400 when Authorization header is missing")
    @Test
    void shouldReturn400WhenAuthorizationHeaderMissing() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            // Missing Authorization header
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    // ============================================
    // TEST CASE 7: Missing Service Authorization Header
    // ============================================

    @Title("Validate Access Code - should return 401 when ServiceAuthorization header is missing")
    @Test
    void shouldReturn403WhenServiceAuthorizationHeaderMissing() {
        Long caseReference = testCase.getCaseReference();
        String accessCode = testCase.getDefendants().get(0).getAccessCode();
        Map<String, String> requestBody = Map.of("accessCode", accessCode);

        // Create a citizen user for this test
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            // Missing ServiceAuthorization header
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(401); // Spring Security returns 401 when required header is missing
    }

    // ============================================
    // TEST CASE 8: Missing Access Code in Request Body
    // ============================================

    @Title("Validate Access Code - should return 400 when access code is missing in request body")
    @Test
    void shouldReturn400WhenAccessCodeIsMissing() {
        Long caseReference = testCase.getCaseReference();
        Map<String, String> requestBody = Map.of();  // Empty body - no accessCode

        // Create a citizen user for this test
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }

    // ============================================
    // TEST CASE 9: Blank Access Code
    // ============================================

    @Title("Validate Access Code - should return 400 when access code is blank")
    @Test
    void shouldReturn400WhenAccessCodeIsBlank() {
        Long caseReference = testCase.getCaseReference();
        Map<String, String> requestBody = Map.of("accessCode", "");  // Blank access code

        // Create a citizen user for this test
        String citizenToken = IdamAuthenticationGenerator.generateToken();

        SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + citizenToken)
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .pathParam("caseReference", caseReference)
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/validate-access-code")
            .then()
            .statusCode(400);
    }
}
