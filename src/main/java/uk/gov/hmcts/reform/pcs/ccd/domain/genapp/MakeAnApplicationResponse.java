package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
public class MakeAnApplicationResponse {

    private final GenAppState state;
    private final String serviceRequestReference;
    private final BigDecimal feeAmount;

}
