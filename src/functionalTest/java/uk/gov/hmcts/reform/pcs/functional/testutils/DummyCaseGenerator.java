package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Slf4j
public class DummyCaseGenerator {


    private static final String baseUrl = System.getenv("TEST_URL");

    public String getCitizenIdamToken() {
        return IdamAuthenticationGenerator.generateToken(IdamAuthenticationGenerator.UserType.citizenUser);
    }

    public String getS2sToken() {
        return new ServiceAuthenticationGenerator().generate(TestConstants.PCS_API);
    }

    public CreateTestCaseResponse createTestCaseWithDefendant() {
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
            .header(TestConstants.AUTHORIZATION, "Bearer " + getCitizenIdamToken())
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

    //Create case with multiple defendants
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

    //Delete case by case reference
    public void deleteTestCase(Long caseReference) {
        try {
            SerenityRest.given()
                .baseUri(baseUrl)
                .header(TestConstants.AUTHORIZATION, "Bearer " + getCitizenIdamToken())
                .header(TestConstants.SERVICE_AUTHORIZATION, getS2sToken())
                .pathParam("caseReference", caseReference)
                .delete("/testing-support/cases/{caseReference}")
                .then()
                .statusCode(204);
        } catch (Exception e) {
            log.error("Failed to delete test case {}: {}", caseReference, e.getMessage());
        }
    }
}
