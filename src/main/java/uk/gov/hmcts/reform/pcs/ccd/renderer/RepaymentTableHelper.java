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
        BigDecimal totalArrears = enforcementCosts.getTotalArrears();
        BigDecimal legalFees = enforcementCosts.getLegalFees();
        BigDecimal landRegistryFees = enforcementCosts.getLandRegistryFees();
        BigDecimal feeAmount = enforcementCosts.getFeeAmount();
        BigDecimal totalFees = BigDecimal.ZERO.add(totalArrears).add(legalFees).add(landRegistryFees).add(feeAmount);

        Map<String, Object> context = new HashMap<>();
        context.put("totalArrears", totalArrears);
        context.put("legalFees", legalFees);
        context.put("landRegistryFees", landRegistryFees);
        context.put(enforcementCosts.getFeeAmountType(), feeFormatter.formatFee(feeAmount));
        context.put("totalFees", totalFees);
        context.put("caption", caption);

        return context;
    }
}
