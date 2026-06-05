package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCardPaymentResponse {

    private String paymentReference;
    private String status;
    private String nextUrl;

}
