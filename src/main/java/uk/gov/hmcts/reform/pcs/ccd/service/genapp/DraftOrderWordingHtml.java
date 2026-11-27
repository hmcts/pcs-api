package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

final class DraftOrderWordingHtml {

    private static final Cleaner STORAGE_CLEANER = new Cleaner(Safelist.none()
        .addTags("b", "blockquote", "br", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                 "hr", "i", "li", "ol", "p", "s", "strike", "strong", "u", "ul")
        .addAttributes(":all", "align", "data-indent")
        .addAttributes("ol", "start", "type")
        .addAttributes("li", "value"));

    private static final Cleaner PDF_CLEANER = new Cleaner(Safelist.basic()
        .addTags("u", "s", "strike")
        .addAttributes("ol", "start")
        .addAttributes("li", "value"));

    private DraftOrderWordingHtml() {
    }

    static String forStorage(String wording) {
        return clean(wording, STORAGE_CLEANER);
    }

    static String forPdf(String wording) {
        return clean(wording, PDF_CLEANER);
    }

    private static String clean(String wording, Cleaner cleaner) {
        if (wording == null || wording.isBlank()) {
            return null;
        }
        var cleanedHtml = cleaner.clean(Jsoup.parseBodyFragment(wording));
        cleanedHtml.outputSettings().prettyPrint(false);
        Element body = cleanedHtml.body();
        return body.text().replace('\u00A0', ' ').isBlank() ? null : body.html();
    }
}
