package uk.gov.hmcts.reform.pcs.feesandpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponse {

    private String code;
    private String description;
    private String version;
    @JsonProperty("fee_amount")
    private BigDecimal feeAmount;
}
