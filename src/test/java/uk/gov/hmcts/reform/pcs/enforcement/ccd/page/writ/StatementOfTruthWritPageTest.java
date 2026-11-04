package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.PlacedFields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementOfTruthWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new StatementOfTruthWritPage());
    }

    @Test
    void shouldKeepTheCertificationOffCheckYourAnswers() {
        Field<?, ?, ?, ?> certification = PlacedFields.find(event.getFields(), "writCertification");

        assertThat(certification.isShowSummary()).isFalse();
    }

    @Test
    void shouldNotRenderTheUnpopulatedPaymentsTable() {
        String markdownField = "writStatementOfTruthRepaymentSummaryMarkdown";

        assertThatThrownBy(() -> PlacedFields.find(event.getFields(), markdownField))
            .isInstanceOf(AssertionError.class);
    }
}
