package uk.gov.hmcts.reform.pcs.functional.tests;


import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.testutils.CaseRoleCleanUp;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;
import uk.gov.hmcts.reform.pcs.functional.testutils.PcsIdamTokenClient;
import uk.gov.hmcts.reform.pcs.functional.testutils.RandomNumberUtil;

import java.util.Map;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeePaymentCallbackTests {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;
    private String accessCode;

    private static final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        accessCode = apiSteps.accessCodeIsFetched(caseReference);
    }

    @AfterAll
    void cleanUp() {
        if (caseReference != null) {
            CaseRoleCleanUp.cleanUpCaseRole(
                caseReference.toString(),
                TestConstants.PCS_SOLICITOR_AUTOMATION_IDAM_UID,
                "[CLAIMANTSOLICITOR]"
            );
        }
    }


    @Title("Fee payment record is persisted successfully when fee payment record callback is triggered with valid data - returns 200")
    @Test
    @Order(1)
    void respondToPossessionClaimStartEventCallbackWithoutAccessCodeAuthTest() {
        String respondClaimRequestBody = PayloadLoader.load(
            "/payloads/repondPossessionClaim-startEventCallbackRequest.json",
            Map.of("caseTypeId", caseType, "caseId", caseReference)
        );

        apiSteps.getFeePaymentDetailsForCaseReference(caseReference);

    }

}
