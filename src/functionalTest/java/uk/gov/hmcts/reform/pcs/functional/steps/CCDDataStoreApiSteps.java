package uk.gov.hmcts.reform.pcs.functional.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CCDDataStoreApiSteps extends ApiSteps<CCDDataStoreApiSteps> {

    private static final String DATA_STORE_URL = System.getenv("DATA_STORE_URL_BASE");
    private static final String caseType = CaseType.getCaseType();

    private String serviceAuthorisationToken;
    private String caseReference;
    private String internalCaseId;
    private String currentEventId;
    private ObjectNode caseData;
    public String eventToken;

    @Override
    protected String baseUrl() {
        return DATA_STORE_URL;
    }

    @Step("initialise CCD data store request")
    public CCDDataStoreApiSteps init() {
        setUp();
        resetHttpState();
        eventToken = null;
        currentEventId = null;
        idamToken = null;
        caseReference = null;
        internalCaseId = null;
        caseData = null;
        this.serviceAuthorisationToken = ccdDataS2sToken;
        return this;
    }

    @Override
    protected CCDDataStoreApiSteps self() {
        return this;
    }

    @Step("using case reference {0}")
    public CCDDataStoreApiSteps withCaseReference(String caseReference) {
        setCaseReference(caseReference);
        return this;
    }

    public String getCaseReference() {
        return this.caseReference;
    }

    public void setCaseReference(String caseReference) {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalArgumentException("Case reference must not be blank");
        }
        this.caseReference = caseReference;
    }

    @Step("create possession claim case")
    public CCDDataStoreApiSteps createPossessionClaimCase(String legislativeCountry)  {

        response = createRequest()
            .pathParam("userId", getCurrentUserId())
            .pathParam("caseType", caseType)
            .when().get("/aggregated/caseworkers/{userId}/jurisdictions/PCS"
                     + "/case-types/{caseType}/event-triggers/createPossessionClaim");
        response.then()
            .statusCode(200)
            .body("event_token", notNullValue())
            .extract().response();
        String eventToken = getResponse().jsonPath().getString("event_token");

        ObjectNode data = parseEventData(PayloadLoader.load("/payloads/ccdEvent/createPossessionClaim.json"));
        LegislativeCountry country = LegislativeCountry.valueOf(legislativeCountry.toUpperCase(Locale.ROOT));
        data.put("legislativeCountry", country.getLabel());
        ObjectNode submitBody = eventRequest(CaseTypeEvents.CREATE_POSSESSION_CLAIM.getEventId(), data, eventToken);
        response = createRequest()
            .pathParam("userId", getCurrentUserId())
            .pathParam("caseType", caseType)
            .body(submitBody)
            .when().post("/caseworkers/{userId}/jurisdictions/PCS/case-types/{caseType}/cases");
        response.then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract().response();
        setCaseReference(getResponse().jsonPath().getString("id"));
        return this;
    }

    @Step("assert can access case details")
    public CCDDataStoreApiSteps assertCanAccessCaseDetails() {
        accessCaseDetails().then().assertThat().statusCode(200);
        return this;
    }

    @Step("assert cannot access case details")
    public CCDDataStoreApiSteps assertCannotAccessCaseDetails() {
        accessCaseDetails().then().assertThat().statusCode(404);
        return this;
    }

    @Step("Fetch case data {0}")
    public CCDDataStoreApiSteps fetchCaseDetails() {
        String caseDataJson = accessCaseDetails()
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .asString();
        try {
            caseData = (ObjectNode) MAPPER.readTree(caseDataJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse case data", e);
        }

        System.out.println("Fetched case data for case reference " + caseReference + ": " + caseData);
        return this;
    }



    public Map<String, String> getDefendantParty(int defendantIndex) {
        JsonNode defendant = caseData.at("/data/allDefendants/" + defendantIndex);
        JsonNode defendantId = defendant.path("id");
        if (defendantId.isMissingNode() || defendantId.isNull()) {
            throw new IllegalStateException(
                "No defendant at index " + defendantIndex + " for case " + caseReference
            );
        }

        Map<String, String> defendantDetails = new HashMap<>();
        defendantDetails.put("id", defendantId.asText());
        defendantDetails.put("firstName", defendant.path("value").path("firstName").asText(null));
        defendantDetails.put("lastName", defendant.path("value").path("lastName").asText(null));
        return defendantDetails;
    }

    public Map<String, String> getClaimantParty(int index) {
        JsonNode claimant = caseData.at("/data/allClaimants/" + index);
        JsonNode claimantId = claimant.path("id");
        if (claimantId.isMissingNode() || claimantId.isNull()) {
            throw new IllegalStateException(
                "No claimant at index " + index + " for case " + caseReference
            );
        }

        Map<String, String> claimantDetails = new HashMap<>();
        claimantDetails.put("id", claimantId.asText());
        claimantDetails.put("orgName", claimant.path("value").path("orgName").asText(null));
        claimantDetails.put("emailAddress", claimant.path("value").path("emailAddress").asText(null));
        return claimantDetails;
    }

    @Step("assert case state is {0}")
    public CCDDataStoreApiSteps assertCaseState(String expectedState) {
        accessCaseDetails()
            .then()
            .assertThat()
            .statusCode(200)
            .body("state", equalTo(expectedState));
        return this;
    }

    @Step("trigger event {0}")
    public CCDDataStoreApiSteps triggerEvent(String eventId) {
        currentEventId = eventId;
        eventToken = null;
        response = createRequest()
            .pathParam("caseReference", this.caseReference)
            .pathParam("eventId", eventId)
            .when().get("/cases/{caseReference}/event-triggers/{eventId}");
        return this;
    }

    @Step("validate trigger event response status is {0}")
    public CCDDataStoreApiSteps validateTriggerEvent(int expectedStatusCode) {
        eventToken = null;
        getResponse().then().statusCode(expectedStatusCode);
        if (expectedStatusCode == 200) {
            String token = getResponse().jsonPath().getString("token");
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Trigger event response has no event token");
            }
            eventToken = token;
        }
        return this;
    }

    @Step("post validate event on page {0}")
    public CCDDataStoreApiSteps postValidateEvent(String pageId, String eventData) {
        if (currentEventId == null || currentEventId.isBlank()) {
            throw new IllegalStateException("No event selected. Call triggerEvent before postValidateEvent.");
        }

        final ObjectNode eventDataObject = parseEventData(eventData);

        ObjectNode requestBody = MAPPER.createObjectNode();
        requestBody.putObject("event")
            .put("id", currentEventId)
            .put("summary", "string")
            .put("description", "string");
        requestBody.putObject("data")
            .put("orgNameFound", "Yes")
            .put("claimantName", "Possession Claims Solicitor Org")
            .put("isClaimantNameCorrect", "YES");
        requestBody.put("ignore_warning", false);
        requestBody.put("case_reference", caseReference);
        requestBody.set("event_data", eventDataObject);

        String acceptVal = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8";

        response = createRequest()
            .pathParam("caseType", caseType)
            .header("Experimental", "True")
            .header("Accept",acceptVal)
            .header("Content-Type","application/json")
            .queryParam("pageId", pageId)
            .body(requestBody)
            .when().post("/case-types/{caseType}/validate");
        return this;
    }

    @Step("verify validate event response status is {0}")
    public CCDDataStoreApiSteps verifyValidateEvent(int expectedStatusCode) {
        getResponse().then().statusCode(expectedStatusCode);
        if (expectedStatusCode != 200) {
            return this;
        }
        getResponse().then()
            .body("errors", anyOf(nullValue(), empty()))
            .extract().response();
        Object responseData = getResponse().jsonPath().get("data");
        JsonNode validatedData = MAPPER.valueToTree(responseData);
        if (!(validatedData instanceof ObjectNode)) {
            throw new IllegalStateException("Event validation did not return case data for " + currentEventId);
        }
        return this;
    }

    @Step("submit event {0} for case")
    public CCDDataStoreApiSteps submitEvent(String event, String eventData) {
        if (eventToken == null || eventToken.isBlank()) {
            throw new IllegalStateException(
                "No event token available. Call triggerEvent and validateTriggerEvent(200) before submitEvent.");
        }

        ObjectNode requestBody = eventRequest(event, parseEventData(eventData), eventToken);

        response = createRequest()
            .pathParam("caseReference", this.caseReference)
            .header("Accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8")
            .header("experimental", true)
            .body(requestBody)
            .when().post("/cases/{caseReference}/events");
        return this;
    }

    @Step("validate submit event response status is {0}")
    public CCDDataStoreApiSteps validateSubmitEvent(int expectedStatusCode) {
        getResponse().then().statusCode(expectedStatusCode);
        return this;
    }

    private static ObjectNode parseEventData(String eventData) {
        if (eventData == null || eventData.isBlank()) {
            throw new IllegalArgumentException("Event data must be a JSON object");
        }
        JsonNode parsedEventData;
        try {
            parsedEventData = MAPPER.readTree(eventData);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse event data as JSON", e);
        }
        if (!(parsedEventData instanceof ObjectNode eventDataObject)) {
            throw new IllegalArgumentException("Event data must be a JSON object");
        }
        return eventDataObject;
    }

    private static ObjectNode eventRequest(String event, ObjectNode data, String token) {
        ObjectNode requestBody = MAPPER.createObjectNode();
        requestBody.set("data", data);
        requestBody.putObject("event")
            .put("id", event)
            .put("summary", "")
            .put("description", "");
        requestBody.put("event_token", token);
        requestBody.put("ignore_warning", false);
        return requestBody;
    }

    public String getCaseId() {
        return caseReference;
    }

    public String getCaseInternalId() {
        String liveCaseNoteToken = getResponse().jsonPath().getString("token");
        DecodedJWT decodedJWT = JWT.decode(liveCaseNoteToken);
        internalCaseId = decodedJWT.getClaim("case-id").asString();
        return internalCaseId;
    }

    public String getClaimantPartyId() {
        fetchCaseDetails();
        if (caseData == null) {
            return null;
        }

        JsonNode claimantId = caseData.at("/data/allClaimants/0/id");
        if (claimantId.isMissingNode() || claimantId.isNull()) {
            return null;
        }

        return claimantId.asText();
    }


    public String getCaseVersion() {
        return getResponse().jsonPath().getString("case_details.version");
    }

    public Response accessCaseDetails() {
        response = createRequest()
            .pathParam("caseReference", this.caseReference)
            .header("experimental", "true")
            .when().get("/cases/{caseReference}");
        return response;
    }

    private RequestSpecification createRequest() {
        if (serviceAuthorisationToken == null) {
            setUp();
            serviceAuthorisationToken = ccdDataS2sToken;
        }
        RequestSpecification newRequest = newRequest()
            .header(TestConstants.SERVICE_AUTHORIZATION, this.serviceAuthorisationToken);
        if (this.idamToken != null) {
            newRequest = newRequest.header(TestConstants.AUTHORIZATION, "Bearer " + this.idamToken);
        }
        return newRequest;
    }


    @Step("expect status {0}")
    public CCDDataStoreApiSteps expectStatus(int statusCode) {
        checkStatusCode(statusCode);
        return this;
    }

    @Step("retrieving internal details from ccd data store")
    public Map<String, String> getInternalCaseDetails(Long caseReference) {
        setUp();
        response = newRequest()
            .header(TestConstants.AUTHORIZATION, "Bearer " + idamTokenFor(TestUsers.UserType.citizenUser))
            .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
            .header("Experimental", "True")
            .pathParam("caseReference", caseReference)
            .when().get("/cases/{caseReference}/event-triggers/addCaseNote");
        response.then().statusCode(200);
        return Map.of(
            "case-id", JWT.decode(getResponse().jsonPath().getString("token")).getClaim("case-id").asString(),
            "case-version", getResponse().jsonPath().getString("case_details.version")
        );
    }

    @Step("Validate event data")
    public Response validateEventData(String caseType, TestUsers.UserType userType,
                                      String eventPageId, Object body) {
        setUp();
        response = newRequest()
            .header(TestConstants.AUTHORIZATION, "Bearer " + idamTokenFor(userType))
            .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
            .header("Experimental", "True")
            .header("Accept",
                    "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8")
            .pathParam("caseType", caseType)
            .queryParam("pageId", eventPageId)
            .body(body)
            .when().post("/case-types/{caseType}/validate");
        response.then().statusCode(200);
        return response;
    }


}
