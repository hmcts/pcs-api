package uk.gov.hmcts.reform.pcs.ccd.page;

import java.beans.Introspector;

public interface CcdPage {

    String getPageKey();

    static String derivePageKey(Class<? extends CcdPage> clazz) {
        String pageKey = clazz.getSimpleName().replaceFirst("Page$", "");
        return Introspector.decapitalize(pageKey);
    }
}
