package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementCosts {

    private BigDecimal totalArrears;
    private BigDecimal legalFees;
    private BigDecimal landRegistryFees;
    private BigDecimal feeAmount;
    private String feeAmountType;
}
