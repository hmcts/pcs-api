package uk.gov.hmcts.reform.pcs.feesandpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {

    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;
    @JsonProperty("payment_reference")
    private String paymentReference;
    @JsonProperty("payment_method")
    private String paymentMethod;
    @JsonProperty("case_reference")
    private String caseReference;
    @JsonProperty("account_number")
    private String accountNumber;

}
