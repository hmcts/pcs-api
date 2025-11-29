package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

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

    private boolean hasZeroPence(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0;
    }
}
