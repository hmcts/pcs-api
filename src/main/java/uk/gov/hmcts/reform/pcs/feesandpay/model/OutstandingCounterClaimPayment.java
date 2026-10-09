package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OutstandingCounterClaimPayment {

    private final String serviceRequestReference;
    private final BigDecimal feeAmount;
    private final String counterClaimAmountInPence;
    private final String counterClaimType;
}
