package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService.EvictionDetails;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the read only "Eviction details" table shown on the schedule and reschedule eviction screens.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvictionDetailsMarkup {

    static final String NOT_KNOWN = "Not known";

    private static final String ROW = "<div class=\"govuk-summary-list__row\">"
        + "<dt class=\"govuk-summary-list__key\">%s</dt>"
        + "<dd class=\"govuk-summary-list__value\">%s</dd></div>\n";

    public static String render(EvictionDetails details) {
        String claimantType = details.claimantType() == null
            ? NOT_KNOWN
            : HtmlUtils.htmlEscape(details.claimantType());
        return """
            <h2 class="govuk-heading-m">Eviction details</h2>
            <dl class="govuk-summary-list">
            %s%s%s</dl>
            """.formatted(
            row("Claimant type", claimantType),
            row("Claimant name", lines(details.claimantNames())),
            row("Defendant names", lines(details.defendantNames()))
        );
    }

    private static String row(String key, String value) {
        return ROW.formatted(key, value);
    }

    private static String lines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return NOT_KNOWN;
        }
        return values.stream()
            .map(HtmlUtils::htmlEscape)
            .collect(Collectors.joining("<br>"));
    }
}
