package uk.gov.hmcts.reform.pcs.payment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponse {
    @JsonProperty("code")
    private String serviceRequestReference;
    private String description;
    private String version;
    @JsonProperty(value = "fee_amount")
    private BigDecimal amount;
}
