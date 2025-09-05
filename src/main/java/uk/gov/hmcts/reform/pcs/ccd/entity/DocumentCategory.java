package uk.gov.hmcts.reform.pcs.ccd.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentCategory implements HasLabel {

    CATEGORY_A("Category_A"),
    CATEGORY_B("Category_B");

    private final String label;
}
