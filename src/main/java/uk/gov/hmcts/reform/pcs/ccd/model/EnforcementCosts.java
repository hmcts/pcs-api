package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementCosts {

    private String totalArrearsPence;
    private String legalFeesPence;
    private String landRegistryFeesPence;
    private String feeAmount;
    private String feeAmountType;
}
