package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class ReferApplicationToJudgeTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ReferApplicationToJudge());
    }

    @Test
    void shouldNotShowPageForSuspendApplications() {
        assertThat(event.getFields().getPageShowConditions())
            .containsEntry(
                "referApplicationToJudge",
                "enter_genapp_ApplicationTypeOption!=\"SUSPEND\" AND "
                    + "enter_genapp_AllPartiesAgree=\"NO\" AND "
                    + "enter_genapp_WithoutNotice=\"YES\""
            );
    }
}
