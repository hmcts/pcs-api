package uk.gov.hmcts.reform.pcs;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClaimIssuePaymentSystemEventTest extends CftlibTest {

    private static final String EVENT_ID = "claimIssuePayment";
    private static final String SYSTEM_USER_ID = "78acf0a0-079b-3112-8cad-549c81b83510";

    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private IdamClient idamClient;

    @Test
    void issuesTheCaseWithOneSystemEventAndReplaysTheCallbackAsANoOp() throws Exception {
        long caseReference = caseCreationService.createMinimalCase(
            idamClient.getAccessToken("pcs-solicitor1@test.com", "password")
        );
        Map<String, Object> caseParameters = Map.of("caseReference", caseReference);

        // the fees-and-pay task creates the service request asynchronously after submission
        await().untilAsserted(() -> assertThat(serviceRequestReference(caseParameters)).isNotNull());
        String serviceRequestReference = serviceRequestReference(caseParameters);

        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestReference(serviceRequestReference)
            .ccdCaseNumber(String.valueOf(caseReference))
            .serviceRequestStatus("Paid")
            .payment(Payment.builder().paymentReference("RC-TEST-0001").build())
            .build();

        runInBackground(() -> paymentService.processPaymentResponse(callback));

        Map<String, Object> event = jdbcTemplate.queryForMap(
            """
                select ce.event_id, ce.event_name, ce.user_id, ce.state_id
                  from ccd.case_event ce
                  join ccd.case_data cd on cd.id = ce.case_data_id
                 where cd.reference = :caseReference
                   and ce.event_id = :eventId
                """,
            Map.of("caseReference", caseReference, "eventId", EVENT_ID)
        );
        assertThat(event)
            .containsEntry("event_name", "Payment Confirmation")
            .containsEntry("user_id", SYSTEM_USER_ID)
            .containsEntry("state_id", "CASE_ISSUED");

        assertThat(jdbcTemplate.queryForObject(
            "select state from ccd.case_data where reference = :caseReference",
            caseParameters, String.class)).isEqualTo("CASE_ISSUED");
        assertThat(paidFeeCount(caseParameters)).isEqualTo(1);
        assertThat(issuedClaimCount(caseParameters)).isEqualTo(1);

        // CCPay re-fires callbacks: the replay must record nothing twice
        runInBackground(() -> paymentService.processPaymentResponse(callback));

        assertThat(jdbcTemplate.queryForObject(
            """
                select count(*)
                  from ccd.case_event ce
                  join ccd.case_data cd on cd.id = ce.case_data_id
                 where cd.reference = :caseReference
                   and ce.event_id = :eventId
                """,
            Map.of("caseReference", caseReference, "eventId", EVENT_ID),
            Integer.class
        )).isEqualTo(1);
    }

    private String serviceRequestReference(Map<String, Object> caseParameters) {
        return jdbcTemplate.query(
            """
                select fp.service_request_reference
                  from fee_payment fp
                  join claim c on fp.possession_claim_id = c.id
                  join pcs_case pc on c.case_id = pc.id
                 where pc.case_reference = :caseReference
                """,
            caseParameters,
            rs -> rs.next() ? rs.getString(1) : null
        );
    }

    private int paidFeeCount(Map<String, Object> caseParameters) {
        return jdbcTemplate.queryForObject(
            """
                select count(*)
                  from fee_payment fp
                  join claim c on fp.possession_claim_id = c.id
                  join pcs_case pc on c.case_id = pc.id
                 where pc.case_reference = :caseReference
                   and fp.status = 'PAID'
                """,
            caseParameters,
            Integer.class
        );
    }

    private int issuedClaimCount(Map<String, Object> caseParameters) {
        return jdbcTemplate.queryForObject(
            """
                select count(*)
                  from claim c
                  join pcs_case pc on c.case_id = pc.id
                 where pc.case_reference = :caseReference
                   and c.claim_issued_date is not null
                """,
            caseParameters,
            Integer.class
        );
    }

    private void runInBackground(Runnable work) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(work).get();
        } finally {
            executor.shutdown();
        }
    }
}
