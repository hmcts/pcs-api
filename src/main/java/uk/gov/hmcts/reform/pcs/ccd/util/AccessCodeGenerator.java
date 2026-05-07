package uk.gov.hmcts.reform.pcs.ccd.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccessCodeGenerator {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPRSTVWXYZ23456789";

    public String generateAccessCode() {
        return RandomStringUtils.random(12, 0, ALLOWED_CHARS.length(), false, false,
                                        ALLOWED_CHARS.toCharArray(), new SecureRandom());
    }
}
