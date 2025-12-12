package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MoneyConverter {

    private static final String CURRENCY_SYMBOL = "£";

    public String convertPenceToPounds(String penceString) {
        if (penceString == null || penceString.isEmpty()) {
            return CURRENCY_SYMBOL + 0;
        }
        BigDecimal pence = new BigDecimal(penceString.trim());
        BigDecimal pounds = pence.movePointLeft(2);
        if (pounds.stripTrailingZeros().scale() <= 0) {
            return CURRENCY_SYMBOL + pounds.stripTrailingZeros().toPlainString();
        }
        return String.format(CURRENCY_SYMBOL + "%.2f", pounds);
    }

    public String convertPoundsToPence(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0";
        }
        String cleansed = amount.replace(CURRENCY_SYMBOL, "").trim();
        BigDecimal pounds = new BigDecimal(cleansed);
        BigDecimal pence = pounds.movePointRight(2);
        return pence.toPlainString();
    }
}
