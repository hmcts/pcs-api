package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DocumentType implements HasLabel {
    RENT_STATEMENT("Rent Statement", DocumentCategory.RENT_STATEMENT, ""),
    OCCUPATION_LICENCE("Occupation Licence", null, "TEN"),
    TENANCY_LICENCE("Tenancy Licence", DocumentCategory.TENANCY_DOCUMENT, "TEN"),
    NOTICE_SERVED("Notice Served", null, "NOT"),
    WITNESS_STATEMENT("Witness Statement", null, "WIT"),
    TENANCY_AGREEMENT("Tenancy Agreement", DocumentCategory.TENANCY_DOCUMENT, "TEN"),
    LETTER_FROM_CLAIMANT("Letter from Claimant", null, "LET"),
    STATEMENT_OF_SERVICE("Statement of Service", null, "SVC"),
    VIDEO_EVIDENCE("Video Evidence", null, "VID"),
    PHOTOGRAPHIC_EVIDENCE("Photographic Evidence", null, "PHT");

    private final String label;
    private final DocumentCategory category;
    private final String filenamePrefix;

}
