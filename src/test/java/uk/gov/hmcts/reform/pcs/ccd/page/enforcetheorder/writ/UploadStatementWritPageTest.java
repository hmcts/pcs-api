package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class UploadStatementWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new UploadStatementWritPage());
    }

    @Test
    void shouldOnlyShowForAWritOfASuspendedOrderWithMissedPayments() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry("uploadStatementWritPage",
                           "chooseEnforcementType=\"WRIT\" AND enforcementIsSuspendedOrder=\"Yes\""
                               + " AND writDefendantMissedPayments=\"YES\"");
    }
}
