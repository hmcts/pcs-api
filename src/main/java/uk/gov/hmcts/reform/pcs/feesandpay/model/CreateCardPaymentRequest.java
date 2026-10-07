package uk.gov.hmcts.reform.pcs.feesandpay.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateCardPaymentRequest {

    @NotNull
    private BigDecimal amount;
    @NotNull
    private String language;
    @NotNull
    private String returnUrl;

}
