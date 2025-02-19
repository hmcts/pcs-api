package uk.gov.hmcts.reform.pcs;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Test
    public void bootsWithCCD() {
        // Env fully booted with definitions imported
        // Even this empty test is useful, checking that your definitions are valid and import successfully.
        // You can add more tests here to use CCD's APIs, XUI with playwright, etc.
    }
}
