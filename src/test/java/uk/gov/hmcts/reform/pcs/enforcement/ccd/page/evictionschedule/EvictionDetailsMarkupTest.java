package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService.EvictionDetails;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvictionDetailsMarkupTest {

    @Test
    void shouldShowTheClaimantTypeClaimantsAndDefendants() {
        String markup = EvictionDetailsMarkup.render(new EvictionDetails(
            "Private landlord", List.of("Acme Lettings"), List.of("Jo Bloggs", "Person(s) unknown")));

        assertThat(markup)
            .contains("Eviction details")
            .contains("<dt class=\"govuk-summary-list__key\">Claimant type</dt>"
                          + "<dd class=\"govuk-summary-list__value\">Private landlord</dd>")
            .contains("<dd class=\"govuk-summary-list__value\">Acme Lettings</dd>")
            .contains("<dd class=\"govuk-summary-list__value\">Jo Bloggs<br>Person(s) unknown</dd>");
    }

    @Test
    void shouldEscapeNames() {
        String markup = EvictionDetailsMarkup.render(new EvictionDetails(
            null, List.of("<script>"), List.of()));

        assertThat(markup)
            .contains("&lt;script&gt;")
            .doesNotContain("<script>")
            .contains("<dd class=\"govuk-summary-list__value\">Not known</dd>");
    }

    @Test
    void shouldFormatTheConfirmationDateAndTime() {
        assertThat(EvictionConfirmationFormat.format(LocalDateTime.parse("2026-06-25T10:00")))
            .isEqualTo("Thursday 25 June 2026 at 10:00am");
        assertThat(EvictionConfirmationFormat.format(LocalDateTime.parse("2026-07-01T09:30")))
            .isEqualTo("Wednesday 1 July 2026 at 09:30am");
        assertThat(EvictionConfirmationFormat.format(LocalDateTime.parse("2026-07-01T14:05")))
            .isEqualTo("Wednesday 1 July 2026 at 02:05pm");
    }

    @Test
    void shouldTellTheBailiffToPrintTheNoticeOfEviction() {
        String body = EvictionConfirmationFormat.scheduledBody("1234-5678-9101-1121", 1234567891011121L);

        assertThat(body)
            .startsWith("Case number: 1234-5678-9101-1121")
            .contains("You need to")
            .contains("Print the 'notice of eviction' (form N54)")
            .contains("href=\"/cases/case-details/1234567891011121#Case%20File%20View\"")
            .contains("Go to case file view (same service, opens in a new tab)");
    }
}
