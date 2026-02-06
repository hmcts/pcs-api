package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MoneyFormatter {

    public String formatFee(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        if (hasZeroPence(amount)) {
            amount = amount.stripTrailingZeros();
        }

        return "£" + amount.toPlainString();
    }

    public BigDecimal deformatFee(String amount) {
        if (amount == null || !amount.startsWith("£")) {
            return null;
        }

        try {
            return new BigDecimal(amount.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasZeroPence(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0;
    }
}
