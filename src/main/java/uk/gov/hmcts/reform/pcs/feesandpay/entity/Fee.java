package uk.gov.hmcts.reform.pcs.feesandpay.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    private String code;
    private String description;
    private String version;
    @JsonProperty("calculatedAmount")
    private BigDecimal calculatedAmount;
}
