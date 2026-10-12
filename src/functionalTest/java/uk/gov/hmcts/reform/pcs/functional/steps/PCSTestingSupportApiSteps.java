package uk.gov.hmcts.reform.pcs.functional.steps;

import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.Endpoints;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import static org.awaitility.Awaitility.await;
import org.awaitility.core.ConditionTimeoutException;


import net.serenitybdd.annotations.Step;

public class PCSTestingSupportApiSteps extends PCSBaseApiSteps {

    public String pinFetched;
    public Map<String, Object> partiesAccessCodes;
    public ArrayNode feePaymentInfoList;
    public Long testingSupportCaseReference;

    @Steps
    ApiSteps apiSteps;

    @Override
    public void initialise() {
        idamUser = PcsIdamTokenClient.UserType.solicitorUser;
        serviceAuthorisationToken = TestConstants.PCS_API;
        super.initialise();
    }

    @Override
    public PCSTestingSupportApiSteps withUser(String userIdentifier) {
        initialise();
        super.withUser(userIdentifier);
//        apiSteps.withUser(userIdentifier);
        return this;
    }

    @Step("testing support create case for legislative country and payload")
    public PCSTestingSupportApiSteps createCaseForLegislativeCountry(String legislativeCountry, Object payloadMerge) {
        return createCaseForLegislativeCountry(legislativeCountry, payloadMerge, false);
    }

    @Step("testing support create case for {0}, issued: {2}")
    public PCSTestingSupportApiSteps createCaseForLegislativeCountry(
        String legislativeCountry, Object payloadMerge, boolean issueAndGenerateAccessCodes
    ) {
        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            RequestSpecification request = createRequest()
                .pathParam("legislativeCountry", legislativeCountry)
                .queryParam("issueAndGenerateAccessCodes", issueAndGenerateAccessCodes);
            if (payloadMerge != null) {
                request.body(payloadMerge);
            }
            var response = request.when()
                .post(Endpoints.CreateTestCase.getResource());
            if (response.statusCode() == 201) {
                testingSupportCaseReference = response.then()
                    .body("caseId", notNullValue())
                    .extract().jsonPath().getLong("caseId");
                return this;
            }

            if (attempt == maxAttempts) {
                response.then().statusCode(201);
            }
        }
        throw new IllegalStateException("Unexpected retry failure");
    }




    @Step("Get Fee payment info for case {0}")
    public PCSTestingSupportApiSteps getFeePaymentInfo(String caseReference) {
        Callable<ArrayNode> fetchPaymentInfo = () -> {
            String paymentInfoJson = createRequest()
                .pathParam("caseReference", caseReference)
                .when()
                .get(Endpoints.GetPaymentInfoDetails.getResource())
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
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(fetchPaymentInfo, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Fee payment info not available for case: " + caseReference, e
            );
        }
        return this;
    }

    public String getPaymentReferenceForPartyId(String caseReference, String partyId) {
        getFeePaymentInfo(caseReference);
        System.out.println("fee payment unfo: " + feePaymentInfoList);

        if (feePaymentInfoList == null) {
            return null;
        }

        for (JsonNode paymentInfo : feePaymentInfoList) {
            JsonNode taskData = paymentInfo.get("taskData");
            if (taskData != null && taskData.isTextual()) {
                try {
                    taskData = MAPPER.readTree(taskData.asText());
                } catch (Exception e) {
                    continue;
                }
            }
            if (taskData == null || !taskData.isObject()) {
                continue;
            }

            if (partyId.equals(taskData.path("responsiblePartyId").asText())) {
                String serviceRequestReference = paymentInfo.path("serviceRequestReference").asText(null);
                System.out.println("Service Request Reference: " + serviceRequestReference);
                return serviceRequestReference;
            }
        }

        return null;
    }


    @Step("a pin is fetched")
    public PCSTestingSupportApiSteps accessCodeIsFetched(String caseReference) {
        Callable<String> fetchPins = () -> {
            Map<String, Object> pins = createRequest()
                .pathParam("caseReference", caseReference)
                .when()
                .get(Endpoints.GetPins.getResource())
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<Map<String, Object>>() {});

                System.out.println("Fetched pins: " + pins);
            if (pins != null && !pins.isEmpty()) {
                partiesAccessCodes = pins;
                return pins.keySet().iterator().next();
            }
            return null;
        };

        try {
            await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(fetchPins, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Access code not available for case: " + caseReference, e
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
        if (partiesAccessCodes == null || partiesAccessCodes.isEmpty()) {
            accessCodeIsFetched(caseReference);
        }

        String accessCode = null;
        System.out.println("partiesAccessCodes for case " + caseReference + ": " + partiesAccessCodes);
        boolean noPartyDetailsProvided = partyId == null && firstName == null && lastName == null;
        for (Map.Entry<String, Object> pinEntry : partiesAccessCodes.entrySet()) {
            if (!(pinEntry.getValue() instanceof Map<?, ?> partyDetails)) {
                continue;
            }

            Object mappedPartyId = partyDetails.containsKey("party_id")
                ? partyDetails.get("party_id")
                : partyDetails.containsKey("partyId")
                    ? partyDetails.get("partyId")
                    : partyDetails.get("id");
            boolean matchesPartyId = hasValue(partyId)
                && partyId.equals(mappedPartyId == null ? null : mappedPartyId.toString());
            boolean matchesNames = hasValue(firstName)
                && hasValue(lastName)
                && firstName.equals(asString(partyDetails.get("firstName")))
                && lastName.equals(asString(partyDetails.get("lastName")));
            boolean hasAnyPartyDetail = hasValue(asString(mappedPartyId))
                || hasValue(asString(partyDetails.get("firstName")))
                || hasValue(asString(partyDetails.get("lastName")));

            if (matchesPartyId || matchesNames || (noPartyDetailsProvided && !hasAnyPartyDetail)) {
                accessCode = pinEntry.getKey();
                break;
            }
        }

        if (accessCode == null) {
            throw new IllegalStateException(
                "Access code not found for party " + partyId + " in case " + caseReference
            );
        }
        String selectedAccessCode = accessCode;

        String idempotencyKey = UUID.randomUUID().toString();
        Callable<String> validateCode = () -> {
            createRequest()
                .header("Idempotency-Key", idempotencyKey)
                .pathParam("caseReference", caseReference)
                .body(Map.of("accessCode", selectedAccessCode))
                .when()
                .post(Endpoints.ValidateAccessCode.getResource())
                .then()
                .statusCode(200);
            return "Success";
        };

        try {
            return await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(700))
                .ignoreExceptions()
                .until(validateCode, notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException(
                "Validate access code failed: " + caseReference, e
            );
        }
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




}
