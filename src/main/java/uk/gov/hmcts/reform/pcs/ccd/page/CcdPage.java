package uk.gov.hmcts.reform.pcs.ccd.page;

public interface CcdPage {

    String getPageId();

    @SuppressWarnings("rawtypes")
    static String getPageId(Class clazz) {
        String pageId = clazz.getSimpleName();

        if (pageId.endsWith("Page")) {
            pageId = pageId.substring(0, pageId.length() - "Page".length());
        }
        return pageId;
    }
}
