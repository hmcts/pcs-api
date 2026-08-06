package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

/**
 * Booting the cftlib stack imports the generated CCD definition; reaching the test
 * body proves the definition is importable. Also keeps at least one executable test
 * in this source set while the create/citizen journeys are disabled for the
 * capacity-only access experiment.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefinitionImportTest extends CftlibTest {

    @Test
    void definitionImportsSuccessfully() {
    }
}
