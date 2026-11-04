package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.PlacedFields;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementOfTruthPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new StatementOfTruthPage());
    }

    @Test
    void shouldNotRenderTheUnpopulatedPaymentsTable() {
        String markdownField = "warrantStatementOfTruthRepaymentSummaryMarkdown";

        assertThatThrownBy(() -> PlacedFields.find(event.getFields(), markdownField))
            .isInstanceOf(AssertionError.class);
    }
}
