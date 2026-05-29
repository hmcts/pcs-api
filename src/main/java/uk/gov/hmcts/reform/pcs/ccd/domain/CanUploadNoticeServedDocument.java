package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum CanUploadNoticeServedDocument implements HasLabel {

    YES("Yes, I can upload a copy of the notice served"),
    NO("No, I cannot upload a copy of the notice served");

    private final String label;

    CanUploadNoticeServedDocument(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
