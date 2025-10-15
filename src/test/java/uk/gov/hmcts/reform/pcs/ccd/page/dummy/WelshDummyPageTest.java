package uk.gov.hmcts.reform.pcs.ccd.page.dummy;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WelshDummyOptions;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for WelshDummyPage functionality.
 */
class WelshDummyPageTest {

    @Test
    void testWelshDummyOptionsEnum() {
        // Test that the enum values are correctly defined
        assertEquals("Secure", WelshDummyOptions.SECURE.getLabel());
        assertEquals("Standard", WelshDummyOptions.STANDARD.getLabel());
        assertEquals("Other", WelshDummyOptions.OTHER.getLabel());
    }

    @Test
    void testWelshDummyPageConfiguration() {
        // Test that the page configuration can be instantiated
        WelshDummyPage page = new WelshDummyPage();
        assertNotNull(page);
    }

    @Test
    void testPcsCaseWithWelshDummyOption() {
        // Test that PCSCase can hold the Welsh dummy option
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .welshDummyOption(WelshDummyOptions.SECURE)
            .build();

        assertNotNull(caseData);
        assertEquals(LegislativeCountry.WALES, caseData.getLegislativeCountry());
        assertEquals(WelshDummyOptions.SECURE, caseData.getWelshDummyOption());
    }
}
