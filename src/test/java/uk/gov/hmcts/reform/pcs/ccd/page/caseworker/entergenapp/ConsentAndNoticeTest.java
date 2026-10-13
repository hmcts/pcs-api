package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentAndNoticeTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ConsentAndNotice());
    }

    @Test
    void shouldNotShowPageForSuspendApplications() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry("consentAndNotice", "enter_genapp_ApplicationTypeOption!=\"SUSPEND\"");
    }
}
