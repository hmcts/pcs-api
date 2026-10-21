package uk.gov.hmcts.reform.pcs.ccd.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Writes out a RichTextArea answer as plain text for a document field that cannot show HTML, keeping the
 * applicant's paragraphs and the numbers their lists show, since what they wrote may refer to them. An answer
 * without HTML, as the citizen journey gives, is already plain text.
 */
public final class RichTextFormatter {

    private static final Pattern HTML = Pattern.compile("^\\s*<(p|ol|ul|h[1-6]|blockquote|br|hr)[\\s/>]",
                                                        Pattern.CASE_INSENSITIVE);
    private static final String INDENT = "    ";

    private RichTextFormatter() {
    }

    public static boolean isHtml(String value) {
        return value != null && HTML.matcher(value).find();
    }

    public static String toPlainText(String value) {
        if (!isHtml(value)) {
            return value;
        }
        List<String> lines = new ArrayList<>();
        blocks(Jsoup.parseBodyFragment(value).body(), "", lines);
        return String.join("\n", lines).strip();
    }

    private static void blocks(Element parent, String indent, List<String> lines) {
        StringBuilder inline = new StringBuilder();
        for (Node child : parent.childNodes()) {
            if (child instanceof Element element && isBlock(element)) {
                flush(inline, indent, lines);
                // A line break or rule only ends the line before it.
                if ("ol".equals(element.normalName()) || "ul".equals(element.normalName())) {
                    list(element, indent, lines);
                } else if (!isBreak(element)) {
                    blocks(element, indent, lines);
                }
            } else {
                inline.append(child instanceof TextNode text ? text.getWholeText()
                                  : ((Element) child).wholeText());
            }
        }
        flush(inline, indent, lines);
    }

    private static void list(Element list, String indent, List<String> lines) {
        boolean ordered = "ol".equals(list.normalName());
        String type = list.attr("type");
        int number = Math.max(1, parseInt(list.attr("start"), 1));
        String childIndent = indent + INDENT.repeat(Math.max(0, parseInt(list.attr("data-indent"), 0)));
        for (Element item : list.children()) {
            if (!"li".equals(item.normalName())) {
                continue;
            }
            String marker = ordered ? label(number++, type) + ". " : "• ";
            List<String> itemLines = new ArrayList<>();
            blocks(item, "", itemLines);
            for (int i = 0; i < itemLines.size(); i++) {
                String line = itemLines.get(i);
                if (line.isBlank()) {
                    continue;
                }
                lines.add(childIndent + (i == 0 ? marker : INDENT) + line);
            }
        }
    }

    private static int parseInt(String attribute, int otherwise) {
        try {
            return Integer.parseInt(attribute.trim());
        } catch (NumberFormatException e) {
            return otherwise;
        }
    }

    private static String label(int number, String type) {
        return switch (type) {
            case "a" -> String.valueOf((char) ('a' + (number - 1) % 26));
            case "i" -> roman(number);
            default -> String.valueOf(number);
        };
    }

    private static String roman(int number) {
        int[] values = {10, 9, 5, 4, 1};
        String[] numerals = {"x", "ix", "v", "iv", "i"};
        StringBuilder roman = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                roman.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return roman.toString();
    }

    private static boolean isBlock(Element element) {
        return element.isBlock() || isBreak(element);
    }

    private static boolean isBreak(Element element) {
        return "br".equals(element.normalName()) || "hr".equals(element.normalName());
    }

    private static void flush(StringBuilder inline, String indent, List<String> lines) {
        String text = inline.toString().replaceAll("\\s+", " ").strip();
        if (!text.isEmpty()) {
            lines.add(indent + text);
        }
        inline.setLength(0);
    }
}
