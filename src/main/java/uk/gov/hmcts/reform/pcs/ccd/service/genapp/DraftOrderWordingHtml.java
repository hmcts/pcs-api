package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * An applicant's draft order wording as it is safe to show and render: the rich text editor's formatting and lists,
 * nothing that runs or loads anything.
 */
public final class DraftOrderWordingHtml {

    private static final Cleaner CLEANER = new Cleaner(Safelist.basic()
        .addTags("u", "s", "strike")
        .addAttributes("ol", "start")
        .addAttributes("li", "value"));

    private DraftOrderWordingHtml() {
    }

    /** The wording cleaned, or null when it has none or holds no text. */
    public static String clean(String wording) {
        if (wording == null) {
            return null;
        }
        var cleanedHtml = CLEANER.clean(Jsoup.parseBodyFragment(wording));
        cleanedHtml.outputSettings().prettyPrint(false);
        Element body = cleanedHtml.body();
        return body.hasText() ? body.html() : null;
    }
}
