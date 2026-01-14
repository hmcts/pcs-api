package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentType implements HasLabel {
    RENT_STATEMENT("Rent statement"),
    OCCUPATION_LICENSE("Occupation license"),
    TENANCY_LICENSE("Tenancy licence"),
    NOTICE_SERVED("Notice served"),
    WITNESS_STATEMENT("Witness Statement"),
    TENANCY_AGREEMENT("Tenancy Agreement"),
    LETTER_FROM_CLAIMANT("Letter from Claimant"),
    STATEMENT_OF_SERVICE("Statement of Service"),
    VIDEO_EVIDENCE("Video Evidence"),
    PHOTOGRAPHIC_EVIDENCE("Photographic Evidence");

    private final String label;
}
