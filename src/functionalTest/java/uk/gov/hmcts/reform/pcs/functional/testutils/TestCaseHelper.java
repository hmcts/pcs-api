package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper for functional tests - creates test cases, tokens, and cleanup.
 */
public class TestCaseHelper {

    private final String baseUrl;

    public TestCaseHelper(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Create citizen user and get IDAM token (creates unique user per call)
    public String getCitizenIdamToken() {
        return CitizenUserGenerator.createCitizenUserAndGetToken();
    }

    // Get S2S token for pcs_api service
    public String getS2sToken() {
        ServiceAuthenticationGenerator generator = new ServiceAuthenticationGenerator();
        return generator.generate(TestConstants.PCS_API);
    }

    // Create test case with single defendant
    public CreateTestCaseResponse createTestCaseWithDefendant() {
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

        return SerenityRest.given()
            .baseUri(baseUrl)
            .contentType(ContentType.JSON)
            .header(TestConstants.AUTHORIZATION, "Bearer " + getCitizenIdamToken())
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .body(requestBody)
            .when()
            .post("/testing-support/create-case")
            .then()
            .statusCode(201)
            .extract()
            .as(CreateTestCaseResponse.class);
    }

    // Create test case with multiple defendants
    public CreateTestCaseResponse createTestCaseWithMultipleDefendants(int numberOfDefendants) {
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
            .header(TestConstants.AUTHORIZATION, "Bearer " + getCitizenIdamToken())
            .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
            .body(requestBody)
            .when()
            .post("/testing-support/create-case")
            .then()
            .statusCode(201)
            .extract()
            .as(CreateTestCaseResponse.class);
    }

    // Delete test case (best effort - won't fail test if deletion fails)
    public void deleteTestCase(Long caseReference) {
        try {
            SerenityRest.given()
                .baseUri(baseUrl)
                .header(TestConstants.AUTHORIZATION, "Bearer " + getCitizenIdamToken())
                .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
                .pathParam("caseReference", caseReference)
                .when()
                .delete("/testing-support/cases/{caseReference}")
                .then()
                .statusCode(204);
        } catch (Exception e) {
            System.err.println("Warning: Failed to delete test case " + caseReference + ": " + e.getMessage());
        }
    }
}
