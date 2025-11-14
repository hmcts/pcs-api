package uk.gov.hmcts.reform.pcs.ccd.util;

import java.math.BigDecimal;

public class CurrencyFormatter {

    private static final String FEE = "Unable to retrieve";

    public static String formatAsCurrency(BigDecimal amount) {
        if (amount == null) {
            return FEE;
        }
        return "£" + amount.stripTrailingZeros().toPlainString();
    }
}
