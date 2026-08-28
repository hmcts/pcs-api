package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ContactPreferencesSelection implements HasLabel {
    BY_EMAIL("By email"),
    BY_POST("By post");

    private final String label;
}
