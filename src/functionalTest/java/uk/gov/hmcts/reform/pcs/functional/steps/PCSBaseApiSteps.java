package uk.gov.hmcts.reform.pcs.functional.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.awaitility.core.ConditionTimeoutException;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.ServiceAuthenticationGenerator;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

public class PCSBaseApiSteps {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected PcsIdamTokenClient.UserType idamUser = PcsIdamTokenClient.UserType.solicitorUser;
    protected String idamToken;
    protected String serviceAuthorisationToken;
    protected ObjectNode startRequestBody;
    protected ObjectNode submitRequestBody;

    private Response response;

    public void initialise() {
        serviceAuthorisationToken = new ServiceAuthenticationGenerator().generate();
    }

    public void initialise(String startPayloadPath, String submitPayloadPath,
                           Map<String, Object> startReplacements, Map<String, Object> submitReplacements) {
        initialise();
        startRequestBody = null;
        submitRequestBody = null;
        serviceAuthorisationToken = new ServiceAuthenticationGenerator().generate();
        try {
            startRequestBody = (ObjectNode) MAPPER.readTree(
                PayloadLoader.load(startPayloadPath, startReplacements));
            submitRequestBody = (ObjectNode) MAPPER.readTree(
                PayloadLoader.load(submitPayloadPath, submitReplacements));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise request bodies", e);
        }
    }

    public PCSBaseApiSteps withUser(String userIdentifier) {
        idamToken = PcsIdamTokenClient.generateTokenForUser(userIdentifier);
        return this;
    }

    public PCSBaseApiSteps updateSubmitEventBody(String jsonPath, Object value) {
        updateRequestBody(submitRequestBody, jsonPath, value);
        return this;
    }

    public PCSBaseApiSteps updateStartEventBody(String jsonPath, Object value) {
        updateRequestBody(startRequestBody, jsonPath, value);
        return this;
    }

    public PCSBaseApiSteps updateIntenalCaseidInSubmitBody(Object value) {
        updateRequestBody(submitRequestBody, "/internal_case_id", value);
        return this;
    }

    private void updateRequestBody(ObjectNode requestBody, String jsonPath, Object value) {
        if (!jsonPath.startsWith("/")) {
            throw new IllegalArgumentException("JSON path must be a JSON Pointer starting with '/': " + jsonPath);
        }

        int lastSeparator = jsonPath.lastIndexOf('/');
        String parentPath = jsonPath.substring(0, lastSeparator);
        String fieldName = jsonPath.substring(lastSeparator + 1);
        JsonNode parent = parentPath.isEmpty() ? requestBody : requestBody.at(parentPath);
        if (!(parent instanceof ObjectNode objectNode) || fieldName.isEmpty()) {
            throw new IllegalArgumentException("JSON path does not identify an object property: " + jsonPath);
        }

        objectNode.set(fieldName, MAPPER.valueToTree(value));
    }

    public void startEventCallback(String eventId) {
        response = createRequest()
            .queryParam("eventId", eventId)
            .body(startRequestBody)
            .when()
            .post(Endpoints.StartEventCallback.getResource());
        response.then().assertThat().statusCode(200);
    }

    public void submitEvent(String eventId) {
        response = createRequest()
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .queryParam("eventId", eventId)
            .body(submitRequestBody)
            .when()
            .post(Endpoints.SubmitEventCallback.getResource());
        response.then().assertThat().statusCode(200);
    }

    @Step("update payment")
    public PCSBaseApiSteps updatePayment(String caseReference, String paymentReference) {

        System.out.println("Updating payment for caseReference: " + caseReference + ", paymentReference: " + paymentReference);
        String paymentUpdateRequest = PayloadLoader.load(
            "/payloads/paymentUpdateRequest.json",
            Map.of(
                "serviceRequestReference", paymentReference,
                "ccdCaseNumber", caseReference
            )
        );

        response = createRequest()
            .body(paymentUpdateRequest)
            .when()
            .put("/payment-update");
        response.then().assertThat().statusCode(204);
        return this;
    }


    @Step("access code validated")
    public PCSBaseApiSteps validateAccessCode(String caseReference, String accessCode) {
        String idempotencyKey = UUID.randomUUID().toString();
        Callable<String> validateCode = () -> {
            createRequest()
                .header("Idempotency-Key", idempotencyKey)
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", accessCode))
                .when()
                .post(Endpoints.ValidateAccessCode.getResource())
                .then()
                .statusCode(200);
            return "Success";
        };

        try {
            await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(validateCode, notNullValue());
            return this;
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Validate access code failed: " + caseReference, e
            );
        }
    }

    protected RequestSpecification createRequest() {
        String userToken = idamToken == null ? PcsIdamTokenClient.generateToken(idamUser) : idamToken;
        return SerenityRest.given()
            .baseUri(System.getenv("TEST_URL"))
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, serviceAuthorisationToken)
            .header(TestConstants.AUTHORIZATION, "Bearer " + userToken);
    }






}
