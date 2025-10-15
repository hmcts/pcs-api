package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for ReasonsForPossessionWales functionality.
 */
class ReasonsForPossessionWalesTest {

    @Test
    void testReasonsForPossessionWalesConfiguration() {
        // Test that the page configuration can be instantiated
        ReasonsForPossessionWales page = new ReasonsForPossessionWales();
        assertNotNull(page);
    }

    @Test
    void testPcsCaseWithWalesLegislativeCountry() {
        // Test that PCSCase can hold the Welsh legislative country
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        assertNotNull(caseData);
        assertNotNull(caseData.getLegislativeCountry());
    }
}
