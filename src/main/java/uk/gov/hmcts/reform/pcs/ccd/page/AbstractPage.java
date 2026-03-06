package uk.gov.hmcts.reform.pcs.ccd.page;

import org.springframework.stereotype.Component;

@Component
public abstract class AbstractPage {

    public String getPageId() {
        String pageId = this.getClass().getSimpleName();

        if (pageId.endsWith("Page")) {
            pageId = pageId.substring(0, pageId.length() - "Page".length()); // -> "AbstractVulnerableAdultsChildren"
        }
        return pageId;
    }
}
