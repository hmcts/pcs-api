package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps.pcsApiS2sToken;
import static uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps.solicitorUserIdamToken;
import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

public class CallbackCaseCreationUtil {

    private static final String BASE_URL = getEnv("TEST_URL");
    private static final String CASE_TYPE = "PCS-1441";

    public static synchronized Long generateCase(Long caseId) {

        String createPossessionClaimRequestBody = PayloadLoader.load(
            "/payloads/createPossessionClaim-submitEventCallbackRequest.json",
            Map.of("caseTypeId", CASE_TYPE, "caseId", caseId)
        );

        String resumePossessionClaimRequestBody = PayloadLoader.load(
            "/payloads/resumePossessionClaim-submitEventCallbackRequest.json",
            Map.of("caseTypeId", CASE_TYPE, "caseId", caseId)
        );

        postEvent(createPossessionClaimRequestBody, "createPossessionClaim");
        postEvent(resumePossessionClaimRequestBody, "resumePossessionClaim");

        return caseId;
    }


    public static void postEvent(Object payload, String eventId) {

        String idempotencyKey = UUID.randomUUID().toString();

        SerenityRest
            .given()
            .baseUri(BASE_URL)
            .contentType(ContentType.JSON)
            .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
            .header(TestConstants.AUTHORIZATION, "Bearer " + solicitorUserIdamToken)
            .header("Idempotency-Key", idempotencyKey)
            .queryParam("eventId", eventId)
            .body(payload)
            .when()
            .post("/ccd-persistence/cases")
            .then()
            .assertThat()
            .statusCode(200);

        int statusCode = SerenityRest.lastResponse().statusCode();

        if (statusCode != 200) {
            throw new RuntimeException(String.format(
                "Request failed. Status code: %d%nResponse: %s",
                statusCode,
                SerenityRest.lastResponse().prettyPrint()
            ));
        }

        assertThat(statusCode).isEqualTo(200);
    }
}
