package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class UploadStatementPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new UploadStatementPage());
    }

    @Test
    void shouldOnlyShowForAWarrantOfASuspendedOrderWithMissedPayments() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry("uploadStatementPage",
                           "chooseEnforcementType=\"WARRANT\" AND enforcementIsSuspendedOrder=\"Yes\""
                               + " AND warrantDefendantMissedPayments=\"YES\"");
    }
}
