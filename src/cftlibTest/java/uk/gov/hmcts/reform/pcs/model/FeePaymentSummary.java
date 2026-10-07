package uk.gov.hmcts.reform.pcs.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.math.BigDecimal;

@Data
@Builder
public final class FeePaymentSummary {

    private final String serviceRequestReference;
    private final BigDecimal amount;
    private final PaymentStatus paymentStatus;

}
