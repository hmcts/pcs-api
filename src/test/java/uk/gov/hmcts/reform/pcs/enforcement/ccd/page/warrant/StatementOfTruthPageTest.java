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

    /**
     * Nothing in src/main sets statementOfTruthRepaymentSummaryMarkdown, so the table it fed was always
     * empty for a real claimant.
     */
    @Test
    void shouldNotRenderTheUnpopulatedPaymentsTable() {
        String markdownField = "warrantStatementOfTruthRepaymentSummaryMarkdown";

        assertThatThrownBy(() -> PlacedFields.find(event.getFields(), markdownField))
            .isInstanceOf(AssertionError.class);
    }
}
