package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class MoneyConverter {

    public BigDecimal convertPenceToBigDecimal(String penceString) {
        if (!StringUtils.hasText(penceString)) {
            return BigDecimal.ZERO;
        }

        BigDecimal pence = new BigDecimal(penceString.trim());

        return pence.movePointLeft(2);
    }

    public String convertPoundsToPence(String amount) {
        if (!StringUtils.hasText(amount)) {
            return "0";
        }
        BigDecimal pounds = new BigDecimal(amount);
        BigDecimal pence = pounds.movePointRight(2);
        return pence.toPlainString();
    }

    public String getTotalPence(String... pennies) {
        long totalPence = 0;
        for (String penceStr : pennies) {
            if (StringUtils.hasText(penceStr)) {
                long pence = Long.parseLong(penceStr);
                totalPence += pence;
            }
        }
        return String.valueOf(totalPence);
    }
}
