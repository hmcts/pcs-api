package uk.gov.hmcts.reform.pcs.testingsupport.model;

import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FeePaymentInfo(
    Integer id,
    String serviceRequestReference,
    LocalDateTime requestDate,
    BigDecimal amount,
    PaymentStatus paymentStatus
) {
}
