package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentType implements HasLabel {
    ADDITIONAL("Additional Document"),
    RENT_STATEMENT("Rent statement"),
    OCCUPATION_LICENSE("Occupation license"),
    TENANCY_LICENSE("Tenancy licence"),
    NOTICE_SERVED("Notice served");

    private final String label;
}
