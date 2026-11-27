package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class MissedPaymentsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new MissedPaymentsPage());
    }

    @Test
    void shouldOnlyShowForAWarrantOfASuspendedOrder() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry("missedPayments",
                           "chooseEnforcementType=\"WARRANT\" AND enforcementIsSuspendedOrder=\"Yes\"");
    }
}
