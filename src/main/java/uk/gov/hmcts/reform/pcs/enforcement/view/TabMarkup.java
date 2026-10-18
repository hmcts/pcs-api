package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GOV.UK markup for the enforcement tabs. XUI renders a tab label as markdown, so the markup has no blank
 * lines: a blank line would end the HTML block and turn the rest into markdown.
 */
final class TabMarkup {

    /**
     * "15 March 2026".
     */
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    /**
     * "12/05/2026".
     */
    static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK);

    /**
     * "08:00 AM". Locale.UK gives a lower case marker, so it is spelled out.
     */
    static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
        .appendPattern("hh:mm ")
        .appendText(ChronoField.AMPM_OF_DAY, Map.of(0L, "AM", 1L, "PM"))
        .toFormatter(Locale.UK);

    record Row(String key, String value) {
    }

    private TabMarkup() {
    }

    static String heading(int level, String text) {
        String size = level <= 2 ? "l" : "m";
        return "<h%d class=\"govuk-heading-%s\">%s</h%d>".formatted(level, size, escape(text), level);
    }

    static String paragraph(String text) {
        return "<p class=\"govuk-body\">" + escape(text) + "</p>";
    }

    static String summaryList(List<Row> rows) {
        return rows.stream()
            .map(row -> "<div class=\"govuk-summary-list__row\">"
                + "<dt class=\"govuk-summary-list__key\">" + escape(row.key()) + "</dt>"
                + "<dd class=\"govuk-summary-list__value\">" + escape(row.value()) + "</dd></div>")
            .collect(Collectors.joining("\n", "<dl class=\"govuk-summary-list\">\n", "\n</dl>"));
    }

    /**
     * A GOV.UK table with a header row. The first cell of each row is its row header.
     */
    static String table(List<String> headers, List<List<String>> rows) {
        String head = headers.stream()
            .map(header -> "<th scope=\"col\" class=\"govuk-table__header\">" + escape(header) + "</th>")
            .collect(Collectors.joining("", "<thead class=\"govuk-table__head\"><tr class=\"govuk-table__row\">",
                                        "</tr></thead>"));
        String body = rows.stream()
            .map(TabMarkup::tableRow)
            .collect(Collectors.joining("\n", "<tbody class=\"govuk-table__body\">\n", "\n</tbody>"));
        return lines(List.of("<table class=\"govuk-table\">", head, body, "</table>"));
    }

    private static String tableRow(List<String> cells) {
        StringBuilder row = new StringBuilder("<tr class=\"govuk-table__row\">");
        for (int index = 0; index < cells.size(); index++) {
            row.append(index == 0
                ? "<th scope=\"row\" class=\"govuk-table__header\">" + escape(cells.get(index)) + "</th>"
                : "<td class=\"govuk-table__cell\">" + escape(cells.get(index)) + "</td>");
        }
        return row.append("</tr>").toString();
    }

    static String sectionBreak() {
        return "<hr class=\"govuk-section-break govuk-section-break--l govuk-section-break--visible\">";
    }

    static String lines(List<String> blocks) {
        return String.join("\n", blocks);
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }

    static String date(LocalDateTime dateTime) {
        return DATE.format(dateTime);
    }

    static String time(LocalDateTime dateTime) {
        return TIME.format(dateTime);
    }

    /**
     * Escapes text for the markup. Line breaks become {@code <br>} so that free text cannot end the HTML block,
     * and {@code $} is escaped so that CCD does not read {@code ${...}} in user text as a field placeholder.
     */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(text)
            .replace("$", "&#36;")
            .replace("\r\n", "\n")
            .replace("\n", "<br>");
    }
}
