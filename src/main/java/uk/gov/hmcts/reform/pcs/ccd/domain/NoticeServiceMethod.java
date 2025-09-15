package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the different methods of serving notice to defendants.
 * Used for radio button selection in the Notice Details page.
 */
public enum NoticeServiceMethod implements HasLabel {

    FIRST_CLASS_POST("By first class post or other service which provides for delivery on the next business day"),
    DELIVERED_PERMITTED_PLACE("By delivering it to or leaving it at a permitted place"),
    PERSONALLY_HANDED("By personally handing it to or leaving it with someone"),
    EMAIL("By email"),
    OTHER_ELECTRONIC("By other electronic method"),
    OTHER("Other");

    private final String label;

    NoticeServiceMethod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
