package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccessCodeGenerator {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPRSTVWXYZ23456789";

    public static String generateAccessCode() {
        return RandomStringUtils.random(12, 0, ALLOWED_CHARS.length(), false, false,
                                        ALLOWED_CHARS.toCharArray(), new SecureRandom());
    }
}
