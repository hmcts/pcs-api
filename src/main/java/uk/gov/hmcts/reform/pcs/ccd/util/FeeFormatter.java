package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class FeeFormatter {

    public String formatFee(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        if (hasZeroPence(amount)) {
            amount = amount.stripTrailingZeros();
        }

        return "£" + amount.toPlainString();
    }

    public String getFeeAmountWithoutCurrencySymbol(String feeAmount, String currencySymbol) {
        if (StringUtils.hasText(feeAmount) && feeAmount.startsWith(currencySymbol)) {
            return feeAmount.substring(1);
        }
        return feeAmount;
    }

    private boolean hasZeroPence(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0;
    }
}
