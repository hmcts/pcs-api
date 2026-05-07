package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class FeeDetails implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String description;
    private final BigDecimal feeAmount;
    private final Integer version;

    public static FeeDetails fromFeeLookupResponse(FeeLookupResponseDto feeLookupResponse) {
        return FeeDetails.builder()
            .code(feeLookupResponse.getCode())
            .description(feeLookupResponse.getDescription())
            .feeAmount(feeLookupResponse.getFeeAmount())
            .version(feeLookupResponse.getVersion())
            .build();
    }
}
