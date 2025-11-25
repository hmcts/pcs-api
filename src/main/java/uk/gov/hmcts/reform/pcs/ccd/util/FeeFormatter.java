package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        return amount.setScale(0, RoundingMode.UP).compareTo(amount) == 0;
    }

}
