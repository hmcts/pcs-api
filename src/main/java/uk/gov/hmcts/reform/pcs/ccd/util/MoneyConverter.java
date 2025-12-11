package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;

@Component
public class MoneyConverter {

    public String convertPenceToPounds(String penceString) {
        if (penceString == null || penceString.isEmpty()) {
            return "£0";
        }

        long pence = Long.parseLong(penceString);
        long pounds = pence / 100;
        long pennies = pence % 100;

        if (pennies == 0) {
            // No pennies, show whole pounds without decimals
            return "£" + pounds;
        } else {
            // Has pennies, show with two decimals
            double amount = pounds + pennies / 100.0;
            return String.format("£%.2f", amount);
        }
    }

    public String convertPoundsToPence(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0";
        }

        String cleansed = amount.replace("£", "").trim();

        if (cleansed.contains(".")) {
            String[] parts = cleansed.split("\\.");
            long pounds = Long.parseLong(parts[0]);
            String penniesStr = parts[1];

            if (penniesStr.length() == 1) {
                penniesStr = penniesStr + "0";
            }

            long pennies = Long.parseLong(penniesStr);

            return String.valueOf(pounds * 100 + pennies);
        } else {
            return String.valueOf(Long.parseLong(cleansed) * 100);
        }
    }
}
