package uk.gov.hmcts.reform.pcs.ccd.domain.documentupload;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum DocumentUploadCategory implements HasLabel {

    ADJOURN_HEARING_APPLICATION,
    SUSPEND_EVICTION_APPLICATION,
    SET_ASIDE_ORDER_APPLICATION,
    GENERAL_APPLICATION;

    @Override
    public String getLabel() {
        return name();
    }
}
