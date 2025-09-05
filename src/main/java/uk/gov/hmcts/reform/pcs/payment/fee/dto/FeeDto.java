package uk.gov.hmcts.reform.pcs.payment.fee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@EqualsAndHashCode
public class FeeDto {
    private String code;

    private String version;

    private Integer volume;

    @JsonProperty("calculated_amount")
    private BigDecimal calculatedAmount;

    private String memoLine;

    private String ccdCaseNumber;

    private String reference;
}
