package uk.gov.hmcts.reform.pcs.ccd.page.dummy;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WelshDummyOptions;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for DummyLicenseDetails functionality.
 */
class DummyLicenseDetailsTest {

    @Test
    void testDummyLicenseDetailsConfiguration() {
        // Test that the page configuration can be instantiated
        DummyLicenseDetails page = new DummyLicenseDetails();
        assertNotNull(page);
    }

    @Test
    void testPcsCaseWithLicenseDetails() {
        // Test that PCSCase can hold the license details
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .welshDummyOption(WelshDummyOptions.SECURE)
            .build();

        assertNotNull(caseData);
        assertEquals(LegislativeCountry.WALES, caseData.getLegislativeCountry());
        assertEquals(WelshDummyOptions.SECURE, caseData.getWelshDummyOption());
    }
}
