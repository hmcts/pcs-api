package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Component
public class MoneyConverter {

    public static final String CURRENCY_SYMBOL = "£";
    private static final ThreadLocal<DecimalFormat> INTEGER_FORMAT =
        ThreadLocal.withInitial(() -> new DecimalFormat("0"));
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT =
        ThreadLocal.withInitial(() -> new DecimalFormat("0.00"));

    public BigDecimal convertPenceToBigDecimal(String penceString) {
        if (penceString == null || penceString.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal pence = new BigDecimal(penceString.trim());

        return pence.movePointLeft(2);
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

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal stripped = amount.stripTrailingZeros();
        DecimalFormat decimalFormat = stripped.scale() <= 0 ? INTEGER_FORMAT.get() : DECIMAL_FORMAT.get();
        return decimalFormat.format(stripped);
    }

}
