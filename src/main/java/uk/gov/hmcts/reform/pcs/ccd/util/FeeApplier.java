package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.function.Consumer;

@Slf4j
@Component
@AllArgsConstructor
public class FeeApplier {

    public static final String UNABLE_TO_RETRIEVE = "Unable to retrieve";

    private final FeeService feeService;
    private final MoneyFormatter moneyFormatter;

    public void applyFeeAmount(FeeType feeType, Consumer<String> setter) {
        try {
            BigDecimal feeAmount = feeService.getFee(feeType).getFeeAmount();
            String formatted = moneyFormatter.formatFee(feeAmount);
            setter.accept(formatted != null ? formatted : UNABLE_TO_RETRIEVE);
        } catch (Exception e) {
            log.error("Error while getting {} fee", feeType.name(), e);
            setter.accept(UNABLE_TO_RETRIEVE);
        }
    }
}
