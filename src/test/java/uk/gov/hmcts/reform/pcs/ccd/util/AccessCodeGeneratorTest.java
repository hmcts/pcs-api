package uk.gov.hmcts.reform.pcs.ccd.util;


import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCodeGeneratorTest {

    private static final String ALLOWED_ACCESS_CODE_CHARS = "ABCDEFGHJKLMNPRSTVWXYZ23456789";

    @Test
    void shouldGenerateCodeOfCorrectLength() {
        String code = AccessCodeGenerator.generateAccessCode();
        assertEquals(12, code.length());
    }

    @Test
    void shouldGenerateValidAccessCode() {
        String code = AccessCodeGenerator.generateAccessCode();
        assertTrue(code.matches("[" + ALLOWED_ACCESS_CODE_CHARS + "]+"));
    }

    @Test
    void shouldGenerateDifferentAccessCodes() {
        String code1 = AccessCodeGenerator.generateAccessCode();
        String code2 = AccessCodeGenerator.generateAccessCode();

        assertNotEquals(code1, code2);
    }
}
