package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

@Slf4j
@Component
@AllArgsConstructor
public class FeeApplier {

    public static final String UNABLE_TO_RETRIEVE = "Unable to retrieve";

    private final FeeService feeService;
    private final FeeFormatter feeFormatter;

    public void applyFormattedFeeAmount(PCSCase pcsCase, FeeType feeType, BiConsumer<PCSCase, String> setter) {
        try {
            BigDecimal feeAmount = feeService.getFee(feeType).getFeeAmount();
            String formatted = feeFormatter.formatFee(feeAmount);
            setter.accept(pcsCase, formatted != null ? formatted : UNABLE_TO_RETRIEVE);
        } catch (Exception e) {
            log.error("Error while getting {} fee", feeType.name(), e);
            setter.accept(pcsCase, UNABLE_TO_RETRIEVE);
        }
    }

    public void applyFeeAmount(PCSCase pcsCase, FeeType feeType, BiConsumer<PCSCase, BigDecimal> setter) {
        try {
            BigDecimal feeAmount = feeService.getFee(feeType).getFeeAmount();
            setter.accept(pcsCase, feeAmount != null ? feeAmount : BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("Error while getting {} fee", feeType.name(), e);
            pcsCase.getEnforcementOrder().setWarrantFeeAmount(BigDecimal.ZERO);
        }
    }
}
