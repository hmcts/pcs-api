package uk.gov.hmcts.reform.pcs.functional.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.Step;
import org.awaitility.core.ConditionTimeoutException;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers.UserType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

public class PCSApiSteps extends ApiSteps<PCSApiSteps> {

    protected String serviceAuthorisationToken;
    public String pinFetched;
    public Map<String, Object> partiesAccessCodes;
    public ArrayNode feePaymentInfoList;
    public Long testingSupportCaseReference;

    private String caseReference;

    @Override
    protected String baseUrl() {
        return System.getenv("TEST_URL");
    }

    public void initialise() {
        setUp();
        resetHttpState();
        caseReference = null;
        idamToken = idamTokenFor(UserType.solicitorUser);
        serviceAuthorisationToken = pcsApiS2sToken;
        pinFetched = null;
        partiesAccessCodes = null;
        feePaymentInfoList = null;
        testingSupportCaseReference = null;
    }

    public PCSApiSteps setCaseReference(String caseReference) {
        this.caseReference = caseReference;
        return this;
    }

    public String getCaseReference() {
        return caseReference;
    }

    @Override
    protected PCSApiSteps self() {
        return this;
    }

    @Override
    public void callIsSubmittedToTheEndpoint(String resource, String method) {
        super.callIsSubmittedToTheEndpoint(
            resource.startsWith("/") ? resource : Endpoints.valueOf(resource).getResource(), method);
    }

    @Step("update payment")
    public PCSApiSteps updatePayment(String serviceRequestRef) {

        System.out.println("Updating payment for caseReference: " + this.caseReference + ", "
                               + "paymentReference: " + serviceRequestRef);
        String paymentUpdateRequest = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of(
                "requestReference", serviceRequestRef,
                "caseReference", this.caseReference
            )
        );

        response = createRequest().body(paymentUpdateRequest)
            .when().put(Endpoints.PaymentUpdate.getResource());
        checkStatusCode(204);
        return this;
    }

    @Step("access code validated")
    public PCSApiSteps validateAccessCode(String caseReference, String accessCode) {
        return validateAccessCodeWithIdentity(caseReference, accessCode, false);
    }

    @Step("access code validated as the shared citizen user")
    public PCSApiSteps validateAccessCodeAsCitizen(String caseReference, String accessCode) {
        setUp();
        return validateAccessCodeWithIdentity(caseReference, accessCode, true);
    }

    private PCSApiSteps validateAccessCodeWithIdentity(
        String caseReference, String accessCode, boolean sharedCitizen
    ) {
        String idempotencyKey = UUID.randomUUID().toString();
        Callable<String> validateCode = () -> {
            RequestSpecification request = sharedCitizen
                ? newRequest()
                    .header(TestConstants.AUTHORIZATION, "Bearer " + idamTokenFor(UserType.citizenUser))
                    .header(TestConstants.SERVICE_AUTHORIZATION, pcsFrontendS2sToken)
                : createRequest();
            response = request
                .header("Idempotency-Key", idempotencyKey)
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", accessCode))
                .when().post(Endpoints.ValidateAccessCode.getResource());
            response.then()
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
        ensureAuthentication();
        return newRequest()
            .header(TestConstants.SERVICE_AUTHORIZATION, serviceAuthorisationToken)
            .header(TestConstants.AUTHORIZATION, "Bearer " + this.idamToken);
    }

    private void ensureAuthentication() {
        setUp();
        if (serviceAuthorisationToken == null) {
            serviceAuthorisationToken = pcsApiS2sToken;
        }
        if (idamToken == null) {
            idamToken = idamTokenFor(UserType.solicitorUser);
        }
    }

    @Step("testing support create case for legislative country and payload")
    public PCSApiSteps createCaseForLegislativeCountry(String legislativeCountry, Object payloadMerge) {
        return createCaseForLegislativeCountry(legislativeCountry, payloadMerge, false);
    }

    @Step("testing support create case for {0}, issued: {2}")
    public PCSApiSteps createCaseForLegislativeCountry(
        String legislativeCountry, Object payloadMerge, boolean issueAndGenerateAccessCodes
    ) {
        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Response response = submitCreateCaseAttempt(
                attempt, legislativeCountry, payloadMerge, issueAndGenerateAccessCodes);
            if (response.statusCode() == 201) {
                testingSupportCaseReference = response.then()
                    .body("caseId", notNullValue())
                    .extract().jsonPath().getLong("caseId");
                setCaseReference(testingSupportCaseReference.toString());
                return this;
            }

            if (attempt == maxAttempts) {
                response.then().statusCode(201);
            }
        }
        throw new IllegalStateException("Unexpected retry failure");
    }

    @Step("Create case attempt {0} for {1}, issued: {3}")
    public Response submitCreateCaseAttempt(
        int attempt, String legislativeCountry, Object payloadMerge, boolean issueAndGenerateAccessCodes
    ) {
        RequestSpecification request = createRequest()
            .pathParam("legislativeCountry", legislativeCountry)
            .queryParam("issueAndGenerateAccessCodes", issueAndGenerateAccessCodes);
        if (payloadMerge != null) {
            request.body(payloadMerge);
        }
        response = request.when().post(Endpoints.CreateTestCase.getResource());
        return response;
    }

    @Step("Get Fee payment info for case")
    public PCSApiSteps getFeePaymentInfo() {
        return fetchFeePaymentInfo();
    }

    private PCSApiSteps fetchFeePaymentInfo() {
        Callable<ArrayNode> fetchPaymentInfo = () -> {
            String paymentInfoJson = submitFeePaymentInfoRequest(getCaseReference())
                .then()
                .statusCode(200)
                .extract()
                .asString();

            ArrayNode paymentInfo;
            try {
                JsonNode paymentInfoNode = MAPPER.readTree(paymentInfoJson);
                paymentInfo = paymentInfoNode != null && paymentInfoNode.isArray()
                    ? (ArrayNode) paymentInfoNode
                    : null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse fee payment info", e);
            }

            System.out.println("paymentInfo: " + paymentInfo);
            if (paymentInfo != null && !paymentInfo.isEmpty()) {
                feePaymentInfoList = paymentInfo;
                return paymentInfo;
            }
            return null;
        };

        try {
            await()
                .pollInSameThread()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(fetchPaymentInfo, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Fee payment info not available for case: " + getCaseReference(), e
            );
        }
        return this;
    }

    @Step("Fetch fee payment info for case {0}")
    public Response submitFeePaymentInfoRequest(String caseReference) {
        response = createRequest()
            .pathParam("caseReference", caseReference)
            .when().get(Endpoints.GetPaymentInfoDetails.getResource());
        return response;
    }

    /**
     * Returns the first service request reference, excluding status records when multiple records exist.
     * The party ID is retained for caller compatibility; fee payment records no longer require task data.
     */
    public String getPaymentReferenceForPartyId(String partyId) {
        if (feePaymentInfoList == null) {
            return null;
        }

        for (JsonNode paymentInfo : feePaymentInfoList) {
            if (feePaymentInfoList.size() > 1 && paymentInfo.has("paymentStatus")) {
                continue;
            }
            JsonNode reference = paymentInfo.get("serviceRequestReference");
            if (reference != null && reference.isTextual()) {
                return reference.asText();
            }
        }

        return null;
    }

    @Step("Fetch access code rest")
    public Map<String, Object> fetchAccessCodesResponse() {
        response = createRequest()
            .pathParam("caseReference", getCaseReference())
            .when().get(Endpoints.GetPins.getResource());
        Map<String, Object> pins = response.then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<Map<String, Object>>() {});

        System.out.println("Fetched pins: " + pins);
        return pins;
    }

    @Step("a pin is fetched")
    public String accessCodeIsFetched() {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalStateException(
                "No case reference set. Call setCaseReference before fetching access codes.");
        }
        Callable<String> fetchPins = () -> {
            Map<String,Object> pins = fetchAccessCodesResponse();
            if (pins != null && !pins.isEmpty()) {
                partiesAccessCodes = pins;
                return pins.keySet().iterator().next();
            }
            return null;
        };
        try {
            pinFetched = await()
                .pollInSameThread()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(fetchPins, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Access code not available for case: " + getCaseReference(), e
            );
        }
        return pinFetched;
    }

    private String validateAndReturnAccessCodeForUser(String firstname, String lastname) {
        if (partiesAccessCodes == null || partiesAccessCodes.isEmpty()) {
            accessCodeIsFetched();
        }
        for (Map.Entry<String, Object> pinEntry : partiesAccessCodes.entrySet()) {
            if (!(pinEntry.getValue() instanceof Map<?, ?> partyDetails)) {
                continue;
            }
            boolean matchesNames = hasValue(firstname)
                && hasValue(lastname)
                && firstname.equals(asString(partyDetails.get("firstName")))
                && lastname.equals(asString(partyDetails.get("lastName")));
            if (matchesNames) {
                return pinEntry.getKey();
            }
        }
        return null;
    }

    @Step("a pin is fetched for user {0} {1}")
    public PCSApiSteps accessCodeIsFetchedForUser(String firstname, String lastname) {
        Callable<String> fetchPins = () -> {
            Map<String,Object> pins = fetchAccessCodesResponse();
            if (pins != null && !pins.isEmpty()) {
                partiesAccessCodes = pins;
                validateAndReturnAccessCodeForUser(firstname, lastname);
                return pins.keySet().iterator().next();
            }
            return null;
        };
        try {
            await()
                .pollInSameThread()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(fetchPins, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Access code not available for case: " + getCaseReference(), e
            );
        }
        return this;
    }

    @Step("access code validated")
    public String validateAccessCodeForParty(
        String caseReference,
        String partyId,
        String firstName,
        String lastName
    ) {
        String selectedAccessCode = validateAndReturnAccessCodeForUser(firstName, lastName);
        if (selectedAccessCode == null) {
            throw new IllegalStateException(
                "No access code available for party " + firstName + " " + lastName + " in case " + caseReference);
        }
        validateAccessCode(caseReference, selectedAccessCode);
        return "Success";
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    public Long getTestingSupportCaseReference() {
        return testingSupportCaseReference;
    }

    @Step("a case for {0} is created")
    public Long ccdCaseIsCreated(String legislativeCountry) {
        return createSharedUserCase(legislativeCountry, false);
    }

    @Step("a case for {0} is created, issued and access codes generated")
    public Long ccdCaseIsCreatedAndIssued(String legislativeCountry) {
        return createSharedUserCase(legislativeCountry, true);
    }

    private Long createSharedUserCase(String legislativeCountry, boolean issueAndGenerateAccessCodes) {
        if (serviceAuthorisationToken == null) {
            initialise();
        }
        String previousIdamToken = idamToken;
        String previousServiceToken = serviceAuthorisationToken;
        try {
            idamToken = idamTokenFor(UserType.solicitorUser);
            serviceAuthorisationToken = pcsApiS2sToken;
            createCaseForLegislativeCountry(legislativeCountry, null, issueAndGenerateAccessCodes);
            return testingSupportCaseReference;
        } finally {
            idamToken = previousIdamToken;
            serviceAuthorisationToken = previousServiceToken;
        }
    }

    @Step("fee payment info details fetched for case {0}")
    public List<Map<String, Object>> getFeePaymentDetailsForCaseReference(Long caseReference) {
        setCaseReference(caseReference.toString());
        fetchFeePaymentInfo();
        return MAPPER.convertValue(feePaymentInfoList, new TypeReference<>() {});
    }
}
