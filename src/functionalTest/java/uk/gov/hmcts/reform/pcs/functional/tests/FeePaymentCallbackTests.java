package uk.gov.hmcts.reform.pcs.functional.tests;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @BeforeEach
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

    Map<String,Object> getClaimantPaymentReference(Long caseReference) {
        apiSteps.requestIsPreparedWithAppropriateValues();
        List<Map<String,Object>> paymentRefs =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);
        assertNotNull(paymentRefs, "Payment references should not be null");
        assertFalse(paymentRefs.isEmpty(), "Payment references should not be empty");
        return paymentRefs.getFirst();
    }

    @Title("Fee Payment callback return 404 for invalid requestReference")
    @Test
    @Disabled("Error validation response to be implement in HDPI-7317")
    void feePaymentWithIncorrectRequestReferenceCallbackFailure() {
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
    @Disabled("Error validation response to be implement in HDPI-7317")
    void feePaymentWithIncorrectCaseReferenceCallbackFailure() {
        Map<String,Object> claimantPaymentRef = getClaimantPaymentReference(caseReference);
        String paymentRequestReference = claimantPaymentRef.get("serviceRequestReference").toString();
        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", "1234123412341234",
                   "requestReference", paymentRequestReference
            )
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(404);
    }

    @Title("Fee Payment callback return 204 on success")
    @Test
    void feePaymentCallbackSuccess() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        apiSteps.requestIsPreparedWithAppropriateValues();

        Map<String,Object> claimantPaymentRef = getClaimantPaymentReference(caseReference);
        String paymentRequestReference = claimantPaymentRef.get("serviceRequestReference").toString();

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", caseReference,
                   "requestReference", paymentRequestReference,
                   "status", "Paid")
        );
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(204);

        Map<String,Object> claimantPaymentRefPostCallback = getClaimantPaymentReference(caseReference);

        String paymentStatus = claimantPaymentRefPostCallback.get("paymentStatus").toString();
        Double paymentAmount = (Double) claimantPaymentRefPostCallback.get("amount");
        assertEquals("PAID",paymentStatus);
        assertEquals(404.00,paymentAmount);
    }

    @Title("Fee Payment callback return 204 on success with payment status Not paid")
    @Test
    void feePaymentCallbackSuccessWithStatusNotPaid() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        apiSteps.requestIsPreparedWithAppropriateValues();

        Map<String,Object> claimantPaymentRef = getClaimantPaymentReference(caseReference);
        String paymentRequestReference = claimantPaymentRef.get("serviceRequestReference").toString();

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", caseReference,
                   "requestReference", paymentRequestReference,
                   "status", "Not paid")
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(204);

        Map<String,Object> claimantPaymentRefPostCallback = getClaimantPaymentReference(caseReference);
        String paymentStatus = claimantPaymentRefPostCallback.get("paymentStatus").toString();

        assertEquals("NOT_PAID",paymentStatus);
    }

    @Title("Fee Payment callback return 204 on success with payment status Partially paid")
    @Test
    void feePaymentCallbackSuccessWithStatusPartiallyPaid() {
        caseReference = apiSteps.ccdCaseIsCreated("england");
        apiSteps.requestIsPreparedWithAppropriateValues();

        List<Map<String,Object>> paymentRefs =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);
        assertNotNull(paymentRefs, "Payment references should not be null");
        assertFalse(paymentRefs.isEmpty(), "Payment references should not be empty");
        Map<String,Object> claimantPayment = paymentRefs.getFirst();
        String requestReference = claimantPayment.get("serviceRequestReference").toString();

        String paymentUpdateRequestBody = PayloadLoader.load(
            "/payloads/payment-update-CallbackRequest.json",
            Map.of("caseReference", caseReference,
                   "requestReference", requestReference,
                   "status", "Partially paid")
        );
        apiSteps.requestIsPreparedWithAppropriateValues();
        apiSteps.theRequestContainsValidServiceToken(TestConstants.PCS_API);
        apiSteps.theRequestContainsBody(paymentUpdateRequestBody);
        apiSteps.callIsSubmittedToTheEndpoint("PaymentUpdate", "PUT");
        apiSteps.checkStatusCode(204);

        List<Map<String,Object>> paymentRefUpdates =  apiSteps.getFeePaymentDetailsForCaseReference(caseReference);
        Map<String,Object> paymentDetails = paymentRefUpdates.getFirst();;

        String paymentStatus = paymentDetails.get("paymentStatus").toString();
        assertEquals("PARTIALLY_PAID",paymentStatus);
    }
}
