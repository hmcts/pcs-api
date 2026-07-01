package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;

@Getter
public enum DocumentAmendFolder implements HasLabel {

    STATEMENTS_OF_CASE(CaseFileCategory.STATEMENTS_OF_CASE),
    PROPERTY_DOCUMENTS(CaseFileCategory.PROPERTY_DOCUMENTS),
    EVIDENCE(CaseFileCategory.EVIDENCE),
    HEARING_DOCUMENTS(CaseFileCategory.HEARING_DOCUMENTS),
    ORDERS_AND_NOTICE_OF_HEARINGS(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS),
    APPLICATIONS(CaseFileCategory.APPLICATIONS),
    APPEALS(CaseFileCategory.APPEALS),
    CORRESPONDENCE(CaseFileCategory.CORRESPONDENCE),
    UNCATEGORISED_DOCUMENTS(CaseFileCategory.UNCATEGORISED_DOCUMENTS);

    private final CaseFileCategory category;
    private final String label;

    DocumentAmendFolder(CaseFileCategory category) {
        this.category = category;
        this.label = category.getLabel();
    }

    public static DocumentAmendFolder from(String value) {
        if (value == null) {
            return null;
        }

        for (DocumentAmendFolder folder : values()) {
            if (folder.name().equals(value)
                || folder.getLabel().equals(value)
                || folder.getCategory().getId().equals(value)) {
                return folder;
            }
        }

        return null;
    }
}
