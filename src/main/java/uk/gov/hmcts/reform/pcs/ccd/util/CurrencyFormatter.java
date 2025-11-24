package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CurrencyFormatter {

    private static final String FEE = "Unable to retrieve";

    public String formatAsCurrency(BigDecimal amount) {
        if (amount == null) {
            return FEE;
        }
        return "£" + amount.stripTrailingZeros().toPlainString();
    }
}
