package uk.gov.hmcts.reform.pcs.ccd.service.order;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * The wording of an order as the frontend exports it from Docweave, kept to the elements of
 * Docweave's output schema: paragraphs, headings, numbered lists, and bold and italic text. The
 * frontend sends only those, but pcs-api cannot tell the HTML came from Docweave, so anything else,
 * such as a link, an image or an attribute, is removed before it reaches a sealed order.
 */
public final class OrderHtml {

    public static final int MAX_LENGTH = 200_000;

    private static final Safelist ORDER_WORDING = new Safelist()
        .addTags("p", "h1", "h2", "h3", "h4", "h5", "h6", "ol", "li", "strong", "em");

    private OrderHtml() {
    }

    /** The wording reduced to the allowed elements, or null when it has no text to issue. */
    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document cleaned = new Cleaner(ORDER_WORDING).clean(Jsoup.parseBodyFragment(html));
        cleaned.outputSettings().prettyPrint(false);
        if (cleaned.body().text().isBlank()) {
            return null;
        }
        numberAsTheEditorShows(cleaned.body());
        return cleaned.body().html();
    }

    /**
     * Numbers the clauses as Docweave's editor shows them, which its HTML leaves to the reader: the
     * order's numbered lists count on from one another, and a list within a clause is numbered i, ii.
     */
    private static void numberAsTheEditorShows(Element body) {
        int next = 1;
        for (Element list : body.children()) {
            if (list.nameIs("ol")) {
                if (next > 1) {
                    list.attr("start", String.valueOf(next));
                }
                next += list.children().size();
            }
        }
        body.select("ol ol").attr("type", "i");
    }
}
