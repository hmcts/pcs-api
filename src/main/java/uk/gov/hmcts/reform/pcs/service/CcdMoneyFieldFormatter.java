package uk.gov.hmcts.reform.pcs.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * CCD expects the MoneyGBP type to be a string with the value in pence.
 */
@Service
public class CcdMoneyFieldFormatter {

    public String formatToPenceString(BigDecimal valueInPounds) {
        if (valueInPounds != null) {
            return valueInPounds.movePointRight(2).toString();
        } else {
            return null;
        }
    }

    public BigDecimal parsePenceString(String valueInPence) {
        if (valueInPence != null) {
            return new BigDecimal(valueInPence).movePointLeft(2);
        } else {
            return null;
        }
    }

}
