package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentCategory implements HasLabel {
    RENT_STATEMENT("Rent Statement"),
    TENANCY_DOCUMENT("Tenancy Documents");

    private final String label;
}
