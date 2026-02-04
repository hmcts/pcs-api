package uk.gov.hmcts.reform.pcs.ccd.renderer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RepaymentTableHelper {

    private final MoneyConverter moneyConverter;
    private final FeeFormatter feeFormatter;

    Map<String, Object> getContext(EnforcementCosts enforcementCosts, String caption) {
        String totalArrearsPence = enforcementCosts.getTotalArrearsPence();
        String legalFeesPence = enforcementCosts.getLegalFeesPence();
        String landRegistryFeesPence = enforcementCosts.getLandRegistryFeesPence();
        String feeAmountPence = moneyConverter.convertPoundsToPence(enforcementCosts.getFeeAmount());
        String totalsPence = moneyConverter.getTotalPence(totalArrearsPence, legalFeesPence, landRegistryFeesPence,
                feeAmountPence);
        BigDecimal totalArrears = moneyConverter.convertPenceToBigDecimal(totalArrearsPence);
        BigDecimal legalFees = moneyConverter.convertPenceToBigDecimal(legalFeesPence);
        BigDecimal landRegistryFees = moneyConverter.convertPenceToBigDecimal(landRegistryFeesPence);
        BigDecimal feeAmount = moneyConverter.convertPenceToBigDecimal(feeAmountPence);
        BigDecimal totalFees = moneyConverter.convertPenceToBigDecimal(totalsPence);

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
