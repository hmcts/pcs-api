package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ContactPreferenceType implements HasLabel {

    POST("Post"),
    EMAIL("Email");

    private final String label;

    @Override
    public String getLabel() {
        return label;
    }
}
