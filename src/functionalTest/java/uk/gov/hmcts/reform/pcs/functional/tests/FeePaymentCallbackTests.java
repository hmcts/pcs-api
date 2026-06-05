package uk.gov.hmcts.reform.pcs.functional.tests;


import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.CaseRoleCleanUp;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FeePaymentCallbackTests extends BaseApi {

    @Steps
    ApiSteps apiSteps;

    private Long caseReference;

    private static final String caseType = CaseType.getCaseType();

    @BeforeAll
    void setUp() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
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

    @Title("Fee Payment callback return 404 for invalid requestReference")
    @Test
    @Order(1)
    void feePaymentWithIncorrectRequestReferenceCallbackFailure() {

        System.out.println("Case Reference: " + caseReference);
        apiSteps.requestIsPreparedWithAppropriateValues();

        List<Map<String,Object>> payementRefs =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);

        System.out.println("payement refs count : " + payementRefs);
        assertNotNull(payementRefs, "Payment references should not be null");
        assertFalse(payementRefs.isEmpty(), "Payment references should not be empty");
        Map<String,Object> claimantPayment = payementRefs.getFirst();
        String requestRefernce = claimantPayment.get("requestReference").toString();
        System.out.println("Payment Reference: " + requestRefernce);

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", caseReference,
                   "requestReference", "INVALID_REFERENCE")
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(404);
    }

    @Title("Fee Payment callback return 404 for invalid requestReference")
    @Test
    @Order(2)
    void feePaymentWithIncorrectCaseReferenceCallbackFailure() {

        System.out.println("Case Reference: " + caseReference);
        apiSteps.requestIsPreparedWithAppropriateValues();

        List<Map<String,Object>> payementRefs =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);

        System.out.println("payement refs count : " + payementRefs);
        assertNotNull(payementRefs, "Payment references should not be null");
        assertFalse(payementRefs.isEmpty(), "Payment references should not be empty");
        Map<String,Object> claimantPayment = payementRefs.getFirst();
        String requestRefernce = claimantPayment.get("requestReference").toString();
        System.out.println("Payment Reference: " + requestRefernce);

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", "1234123412341234",
                   "requestReference", requestRefernce)
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(404);
    }

    @Title("Fee Payment callback return 200 on success")
    @Test
    @Order(2)
    void feePaymentCallbackSuccess() {

        System.out.println("Case Reference: " + caseReference);
        apiSteps.requestIsPreparedWithAppropriateValues();

        List<Map<String,Object>> payementRefs =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);

        System.out.println("payement refs count : " + payementRefs);
        assertNotNull(payementRefs, "Payment references should not be null");
        assertFalse(payementRefs.isEmpty(), "Payment references should not be empty");
        Map<String,Object> claimantPayment = payementRefs.getFirst();
        String requestRefernce = claimantPayment.get("requestReference").toString();
        System.out.println("Payment Reference: " + requestRefernce);

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference",  caseReference,
            "requestReference", requestRefernce)
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(204);
    }

}
