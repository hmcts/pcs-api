package uk.gov.hmcts.reform.pcs.functional.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.EventDataUtil;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;
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
import static uk.gov.hmcts.reform.pcs.functional.steps.PCSBaseApiSteps.MAPPER;

public class CCDDataStoreApiSteps {

    private static final String DATA_STORE_URL = System.getenv("DATA_STORE_URL_BASE");
    private static final String CCD_DATA = "ccd_data";
    private static final String caseType = CaseType.getCaseType();

    private RequestSpecification request;
    private Response response;
    private String currentUserId;
    private String idamToken;
    private String serviceAuthorisationToken;
    private String caseReference;
    private String internalCaseId;
    private String currentEventId;
    private ObjectNode caseData;
    public String event_token;

    @Step("initialise CCD data store request")
    public CCDDataStoreApiSteps init() {
        response = null;
        currentUserId = null;
        event_token = null;
        currentEventId = null;
        serviceAuthorisationToken = new ServiceAuthenticationGenerator().generate(CCD_DATA);
        request = createRequest();
        return this;
    }

    @Step("with user {0}")
    public CCDDataStoreApiSteps withUser(String userIdentifier) {
        idamToken = PcsIdamTokenClient.generateTokenForUser(userIdentifier);
        currentUserId = TestUsers.getIdamId(userIdentifier);
        request = createRequest().header(TestConstants.AUTHORIZATION, "Bearer " + idamToken);
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
            .pathParam("userId", currentUserId)
            .pathParam("caseType", caseType)
            .when()
            .get("/aggregated/caseworkers/{userId}/jurisdictions/PCS"
                     + "/case-types/{caseType}/event-triggers/createPossessionClaim")
            .then()
            .statusCode(200)
            .body("event_token", notNullValue())
            .extract().response();
        String eventToken = response.jsonPath().getString("event_token");

        ObjectNode data = EventDataUtil.forEvent(CaseTypeEvents.CREATE_POSSESSION_CLAIM).getData();
        LegislativeCountry country = LegislativeCountry.valueOf(legislativeCountry.toUpperCase(Locale.ROOT));
        data.put("legislativeCountry", country.getLabel());
        ObjectNode submitBody = eventRequest(CaseTypeEvents.CREATE_POSSESSION_CLAIM.getEventId(), data, eventToken);
        response = createRequest()
            .pathParam("userId", currentUserId)
            .pathParam("caseType", caseType)
            .body(submitBody)
            .when()
            .post("/caseworkers/{userId}/jurisdictions/PCS/case-types/{caseType}/cases")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract().response();
        setCaseReference(response.jsonPath().getString("id"));
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
    public CCDDataStoreApiSteps fetchCaseDetails(String caseReference) {
        setCaseReference(caseReference);
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

   

    public Map<String, String> getDefendantParty(String caseReference, int defendantIndex) {
        fetchCaseDetails(caseReference);
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

    @Step("assert case state is {0}")
    public CCDDataStoreApiSteps assertCaseState(String expectedState) {
        accessCaseDetails()
            .then()
            .assertThat()
            .statusCode(200)
            .body( "state", equalTo(expectedState));
        return this;
    }

    @Step("trigger event {0}")
    public CCDDataStoreApiSteps triggerEvent(String eventId) {
        currentEventId = eventId;
        response = createRequest()
            .pathParam("caseReference", this.caseReference)
            .pathParam("eventId", eventId)
            .when()
            .get("/cases/{caseReference}/event-triggers/{eventId}")
        .then()
            .statusCode(200)
            .extract().response();
        event_token = response.jsonPath().getString("event_token");
        return this;
    }

    @Step("validate event {0} for case {1}")
    public CCDDataStoreApiSteps validateEvent(String pageId, ObjectNode eventData) {
        if (currentEventId == null || currentEventId.isBlank()) {
            throw new IllegalStateException("No event selected. Call triggerEvent before validateEvent.");
        }

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
        requestBody.set("event_data", eventData);

        String acceptVal = "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8";

        Response validationResponse = createRequest()
            .pathParam("caseType", caseType)
            .header("Experimental", "True")
            .header("Accept",acceptVal)
            .header("Content-Type","application/json")
            .queryParam("pageId", pageId)
            .body(requestBody)
            .when()
            .post("/case-types/{caseType}/validate")
            .then()
            .statusCode(200)
            .body("errors", anyOf(nullValue(), empty()))
            .extract().response();
        Object responseData = validationResponse.jsonPath().get("data");
        JsonNode validatedData = MAPPER.valueToTree(responseData);
        if (validatedData instanceof ObjectNode objectData) {
            eventData.setAll(objectData);
        } else {
            throw new IllegalStateException("Event validation did not return case data for " + currentEventId);
        }
        return this;
    }

    public String getClaimantPartyId(String caseReference) {
        fetchCaseDetails(caseReference);
        if (caseData == null) {
            return null;
        }

        JsonNode claimantId = caseData.at("/data/allClaimants/0/id");
        if (claimantId.isMissingNode() || claimantId.isNull()) {
            return null;
        }

        return claimantId.asText();
    }

    @Step("submit event {0} for case {1}")
    public CCDDataStoreApiSteps submitEvent(String event, ObjectNode eventData) {
        if (event_token == null || event_token.isBlank()) {
            throw new IllegalStateException("No event token available. Call triggerEvent before submitEvent.");
        }

        ObjectNode requestBody = eventRequest(event, eventData, event_token);

        response = createRequest()
            .pathParam("caseReference", this.caseReference)
            .header("Accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8")
            .body(requestBody)
            .when()
            .post("/cases/{caseReference}/events")
        .then()
            .statusCode(200)
            .extract().response();
        return this;
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
        String liveCaseNoteToken = response.jsonPath().getString("token");
        DecodedJWT decodedJWT = JWT.decode(liveCaseNoteToken);
        internalCaseId = decodedJWT.getClaim("case-id").asString();
        return internalCaseId;
    }

    public String getCaseVersion() {
        return response.jsonPath().getString("case_details.version");
    }

    public Response accessCaseDetails() {
        response = createRequest()
            .pathParam("caseReference", caseReference)
            .header("experimental",
                    "true")
            .when()
            .get("/cases/{caseReference}");
        return response;
    }

    private RequestSpecification createRequest() {
        RequestSpecification newRequest = SerenityRest.given()
            .baseUri(DATA_STORE_URL)
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, serviceAuthorisationToken);
        if (idamToken != null) {
            newRequest = newRequest.header(TestConstants.AUTHORIZATION, "Bearer " + idamToken);
        }
        return newRequest;
    }


    @Step("expect status {0}")
    public CCDDataStoreApiSteps expectStatus(int statusCode) {
        response.then().assertThat().statusCode(statusCode);
        return this;
    }



}
