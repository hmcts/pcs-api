package uk.gov.hmcts.reform.pcs.service;

import org.awaitility.Awaitility;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Service
public class FeePaymentService {

    private static final String PCS_API_HOST = "http://localhost:3206";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String serviceAuthorisation;

    public FeePaymentService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceAuthorisation = generateTestS2SToken("pcs_api");
    }

    public List<FeePaymentSummary> waitForFeePaymentRequests(long caseReference,
                                                             PaymentCallbackHandlerType paymentType) {
        return Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(1000))
            .until(() -> getFeePaymentsForCase(caseReference, paymentType), not(empty()));
    }

    public void makePaymentCallback(long caseReference,
                                    FeePaymentSummary feePaymentSummary) {

        RestClient restClient = RestClient
            .create(PCS_API_HOST);

        PaymentStatusCallback paymentStatusCallback = PaymentStatusCallback.builder()
            .serviceRequestReference(feePaymentSummary.getServiceRequestReference())
            .serviceRequestStatus(PaymentStatus.PAID.name())
            .serviceRequestAmount(feePaymentSummary.getAmount())
            .ccdCaseNumber(Long.toString(caseReference))
            .payment(Payment.builder()
                         .paymentReference("cftlib-" + UUID.randomUUID())
                         .build())
            .build();

        ResponseEntity<Void> responseEntity = restClient
            .put()
            .uri("/payment-update", caseReference)
            .contentType(APPLICATION_JSON)
            .header("ServiceAuthorization", serviceAuthorisation)
            .body(paymentStatusCallback)
            .retrieve()
            .toBodilessEntity();

        assertThat(responseEntity.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
    }

    private List<FeePaymentSummary> getFeePaymentsForCase(long caseReference,
                                                          PaymentCallbackHandlerType paymentType) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("caseReference", caseReference)
            .addValue("paymentType", paymentType.name());

        return jdbcTemplate.query(
            """
                SELECT fp.service_request_reference, fp.amount, fp.status
                FROM fee_payment fp
                    JOIN claim c ON c.id = fp.possession_claim_id
                    JOIN pcs_case pcs ON pcs.id = c.case_id
                WHERE pcs.case_reference = :caseReference AND payment_callback_handler_type = :paymentType
                """,
            namedParameters,
            (rs, rowNum) -> {
                String statusString = rs.getString("status");
                return FeePaymentSummary.builder()
                        .serviceRequestReference(rs.getString("service_request_reference"))
                        .amount(new BigDecimal(rs.getString("amount")))
                        .paymentStatus(statusString != null ? PaymentStatus.valueOf(statusString) : null)
                        .build();
            }
        );
    }

}
