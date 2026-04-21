package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CaseFileCategory {

    STATEMENTS_OF_CASE("statementsOfCase", "Statements of Case", 1),
    PROPERTY_DOCUMENTS("propertyDocuments", "Property documents", 2),
    EVIDENCE("evidence", "Evidence", 3),
    HEARING_DOCUMENTS("hearingDocuments", "Hearing documents", 4),
    ORDERS_AND_NOTICE_OF_HEARINGS("ordersAndNoticeOfHearings", "Orders and notice of hearings", 5),
    APPLICATIONS("applications", "Applications", 6),
    APPEALS("appeals", "Appeals", 7),
    CORRESPONDENCE("correspondence", "Correspondence", 8),
    UNCATEGORISED("uncategorised", "Uncategorised", 9);

    private final String id;
    private final String label;
    private final int displayOrder;
}
