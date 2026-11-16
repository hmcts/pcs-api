package uk.gov.hmcts.reform.pcs.ccd.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * What a user typed, shown as typed in a markdown tab: one line of HTML, so a blank line cannot end the tab's HTML
 * and leave the rest to markdown, and with the characters markdown and XUI's ${...} placeholders read as entities.
 */
public class PlainTextFilter implements Filter {

    private static final String SPECIAL = "&<>\"'\\`*_[]{}#|~!$";

    @Override
    public List<String> getArgumentNames() {
        return Collections.emptyList();
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context,
                        int lineNumber) {
        return input == null ? null : new SafeString(encode(input.toString()));
    }

    public static String encode(String text) {
        StringBuilder html = new StringBuilder();
        text.replace("\r\n", "\n").replace('\r', '\n').chars().forEach(c -> {
            if (c == '\n') {
                html.append("<br>");
            } else if (SPECIAL.indexOf(c) >= 0) {
                html.append("&#").append(c).append(';');
            } else {
                html.append((char) c);
            }
        });
        return html.toString();
    }
}
