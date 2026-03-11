package uk.gov.hmcts.reform.pcs.ccd.page;

public interface CcdPage {

    String getFieldPrefix();

    static String getFieldPrefix(Class<? extends CcdPage> clazz) {
        return clazz.getSimpleName().replaceFirst("Page$", "");
    }
}
