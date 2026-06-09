package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
public class CounterClaimSubmitResponse {

    private final CounterClaimStatus status;
    private final String serviceRequestReference;
    private final BigDecimal feeAmount;
}
