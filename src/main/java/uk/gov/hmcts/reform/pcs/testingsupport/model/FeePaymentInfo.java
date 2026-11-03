package uk.gov.hmcts.reform.pcs.testingsupport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

@Getter
@Setter
public class FeePaymentInfo {

    private Integer id;
    private String serviceRequestReference;
    private LocalDateTime requestDate;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
}
