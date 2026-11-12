package uk.gov.hmcts.reform.pcs.functional.testutils;

import java.security.SecureRandom;

/**
 * Lifted from `UidService` in ccd-data-store and adjusted to return a long case reference
 * This is so that a valid random CCD case reference can be generated, with the
 * correct checksum.
 */
public class CcdCaseReferenceGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static long generateRandomCaseReference() {
        String currentTime10OfSeconds = String.valueOf(System.currentTimeMillis()).substring(0, 11);
        StringBuilder builder = new StringBuilder(currentTime10OfSeconds);
        for (int i = 0; i < 4; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }
        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = checkSum(builder.toString(), true);
        builder.append(checkDigit);

        return Long.parseLong(builder.toString());
    }

    /**
     * Generate check digit for a number string.
     *
     * @param numberString number string to process
     * @param noCheckDigit Whether check digit is present or not. True if no check Digit
     *                     is appended.
     * @return checkDigit int
     */
    public static int checkSum(String numberString, boolean noCheckDigit) {
        int sum = 0;
        int checkDigit = 0;

        if (!noCheckDigit) {
            numberString = numberString.substring(0, numberString.length() - 1);
        }

        boolean isDouble = true;
        for (int i = numberString.length() - 1; i >= 0; i--) {
            int k = Integer.parseInt(String.valueOf(numberString.charAt(i)));
            sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
            isDouble = !isDouble;
        }

        if ((sum % 10) > 0) {
            checkDigit = (10 - (sum % 10));
        }

        return checkDigit;
    }

    private static int sumToSingleDigit(int k) {
        if (k < 10) {
            return k;
        }

        return sumToSingleDigit(k / 10) + (k % 10);
    }

}
