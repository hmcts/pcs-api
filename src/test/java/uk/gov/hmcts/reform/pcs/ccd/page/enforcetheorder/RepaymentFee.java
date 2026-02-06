package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import java.math.BigDecimal;

public record RepaymentFee(
    String warrantFeeAmount,
    BigDecimal landRegistryAmount,
    BigDecimal legalCostsAmount,
    BigDecimal rentArrearsAmount,
    BigDecimal expectedTotalFees
) {}
