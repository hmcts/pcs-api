package uk.gov.hmcts.reform.pcs.functional.testutils;

import java.util.Random;

public class RandomNumberUtil {

    private static final Random RANDOM = new Random();

    public static long generateRandomNumber(int length) {
        if (length <= 0) return 0;

        StringBuilder sb = new StringBuilder();
        sb.append(RANDOM.nextInt(9) + 1);
        for (int i = 1; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }

        return Long.parseLong(sb.toString());
    }
}
