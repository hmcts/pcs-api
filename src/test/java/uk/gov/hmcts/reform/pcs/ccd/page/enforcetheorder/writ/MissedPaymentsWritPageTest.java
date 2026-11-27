package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class MissedPaymentsWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new MissedPaymentsWritPage());
    }

    @Test
    void shouldOnlyShowForAWritOfASuspendedOrder() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry("missedPaymentsWrit",
                           "chooseEnforcementType=\"WRIT\" AND enforcementIsSuspendedOrder=\"Yes\"");
    }
}
