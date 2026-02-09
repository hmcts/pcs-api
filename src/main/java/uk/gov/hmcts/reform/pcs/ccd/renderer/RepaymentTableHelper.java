package uk.gov.hmcts.reform.pcs.ccd.renderer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RepaymentTableHelper {

    private final FeeFormatter feeFormatter;

    Map<String, Object> getContext(EnforcementCosts enforcementCosts, String caption) {
        BigDecimal totalArrears = safe(enforcementCosts.getTotalArrears());
        BigDecimal legalFees = safe(enforcementCosts.getLegalFees());
        BigDecimal landRegistryFees = safe(enforcementCosts.getLandRegistryFees());
        BigDecimal feeAmount = safe(enforcementCosts.getFeeAmount());
        BigDecimal totalFees = totalArrears.add(legalFees).add(landRegistryFees).add(feeAmount);

        Map<String, Object> context = new HashMap<>();
        context.put("totalArrears", totalArrears);
        context.put("legalFees", legalFees);
        context.put("landRegistryFees", landRegistryFees);
        context.put(enforcementCosts.getFeeAmountType(), feeFormatter.formatFee(feeAmount));
        context.put("totalFees", totalFees);
        context.put("caption", caption);

        return context;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
