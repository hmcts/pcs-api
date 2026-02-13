package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps.pcsApiS2sToken;
import static uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps.solicitorUserIdamToken;

public class CaseRoleCleanUp {

    private static final String dataStoreUrl = System.getenv("DATA_STORE_URL_BASE");

    public static void cleanUpCaseRole(String caseId, String userId, String caseRole) {
        try {
            Map<String, Object> payload = Map.of(
                "case_users", new Map[] {
                    Map.of(
                        "case_id", caseId,
                        "user_id", userId,
                        "case_role", caseRole
                    )
                }
            );

            SerenityRest.given()
                .relaxedHTTPSValidation()
                .baseUri(dataStoreUrl)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(TestConstants.SERVICE_AUTHORIZATION, pcsApiS2sToken)
                .header(TestConstants.AUTHORIZATION, solicitorUserIdamToken)
                .body(payload)
                .when()
                .delete("/case-users")
                .then()
                .statusCode(200);

        } catch (Exception e) {
            System.err.println("Failed to delete case role for caseId=" + caseId + e.getMessage());
        }
    }
}
