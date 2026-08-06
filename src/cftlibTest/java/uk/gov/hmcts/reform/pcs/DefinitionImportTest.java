package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

/**
 * Booting the cftlib stack imports the generated CCD definition; reaching the test
 * body proves the definition is importable. Also keeps at least one executable test
 * in this source set while the create/citizen journeys are disabled for the
 * capacity-only access experiment. The SpringBootTest annotation is load-bearing:
 * this class's application context is the thing that boots the PCS app the cftlib
 * boot latch waits for.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefinitionImportTest extends CftlibTest {

    @Test
    void definitionImportsSuccessfully() {
    }
}
