package uk.gov.hmcts.reform.pcs.ccd.util;


import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCodeGeneratorTest {

    private static final String ALLOWED_ACCESS_CODE_CHARS = "ABCDEFGHJKLMNPRSTVWXYZ23456789";

    private final AccessCodeGenerator accessCodeGenerator = new AccessCodeGenerator();

    @Test
    void shouldGenerateCodeOfCorrectLength() {
        String code = accessCodeGenerator.generateAccessCode();
        assertEquals(12, code.length());
    }

    @Test
    void shouldGenerateValidAccessCode() {
        String code = accessCodeGenerator.generateAccessCode();
        assertTrue(code.matches("[" + ALLOWED_ACCESS_CODE_CHARS + "]+"));
    }

    @Test
    void shouldGenerateDifferentAccessCodes() {
        String code1 = accessCodeGenerator.generateAccessCode();
        String code2 = accessCodeGenerator.generateAccessCode();

        assertNotEquals(code1, code2);
    }
}
