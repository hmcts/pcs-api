package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.AdditionalPayment;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Occupant;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.PaymentLine;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Row;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Section;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BulkClaimDetailsRendererTest {

    private final BulkClaimDetailsRenderer underTest =
        new BulkClaimDetailsRenderer(new PebbleConfiguration().pebbleEngine());

    @Test
    void shouldRenderRentalClaimWithPaymentHistory() {
        BulkClaimDetails details = new BulkClaimDetails(
            List.of(new Row("Customer unique identifier", "CUST-1"), new Row("Schema version", "2.0")),
            List.of(new Section("Claimant: Jane <Doe>", List.of(new Row("DX number", "DX 123")))),
            List.of(new PaymentLine("1 January 2026", "£500", "£100", "£400"),
                    new PaymentLine("1 February 2026", "£500", "£0", "£900")),
            List.of(), List.of(), List.of(), List.of()
        );

        String html = underTest.render(details);

        assertThat(html)
            .contains("Submission")
            .contains("Customer unique identifier")
            .contains("CUST-1")
            .contains("Claimant: Jane &lt;Doe&gt;")
            .contains("DX 123")
            .contains("Arrears payment history")
            .contains("1 January 2026")
            .contains("£900")
            .doesNotContain("Mortgage particulars")
            .doesNotContain("Additional payments")
            .doesNotContain("Original mortgage provider")
            .doesNotContain("Occupants");
    }

    @Test
    void shouldRenderMortgageClaimWithAllSections() {
        BulkClaimDetails details = new BulkClaimDetails(
            List.of(new Row("Schema version", "2.0")),
            List.of(),
            List.of(),
            List.of(new Row("Loan amount", "£150000"), new Row("Start interest rate", "4.5%")),
            List.of(new AdditionalPayment("£25", "Insurance", "£10")),
            List.of(new Row("Name", "First Lender")),
            List.of(new Occupant("Mr John Smith", "Johnny", "JS"))
        );

        String html = underTest.render(details);

        assertThat(html)
            .contains("Mortgage particulars")
            .contains("Loan amount")
            .contains("£150000")
            .contains("4.5%")
            .contains("Additional payments")
            .contains("Insurance")
            .contains("Original mortgage provider")
            .contains("First Lender")
            .contains("Occupants")
            .contains("Mr John Smith")
            .contains("Johnny")
            .doesNotContain("Arrears payment history");
    }
}
