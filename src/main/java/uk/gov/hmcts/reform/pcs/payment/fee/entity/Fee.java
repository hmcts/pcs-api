package uk.gov.hmcts.reform.pcs.payment.fee.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    private String code;
    private String description;
    private String version;
    @JsonProperty("calculatedAmount")
    private BigDecimal calculatedAmount;

    @Override
    public String toString() {
        return "Fee{ calculatedAmount=" + calculatedAmount
            + ", description='" + description + '\''
            + ", version=" + version
            + ", code='" + code + '\'' + '}';
    }
}
