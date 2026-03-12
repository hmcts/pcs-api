package uk.gov.hmcts.reform.pcs.ccd.page;

public interface CcdPage {

    String getPageKey();

    static String derivePageKey(Class<? extends CcdPage> clazz) {
        return clazz.getSimpleName().replaceFirst("Page$", "");
    }
}
