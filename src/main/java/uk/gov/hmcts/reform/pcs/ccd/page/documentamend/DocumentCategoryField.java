package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;

enum DocumentCategoryField {
    STATEMENTS_OF_CASE(
        CaseFileCategory.STATEMENTS_OF_CASE,
        "statementsOfCase",
        DocumentAmendDetails::getStatementsOfCaseDocuments,
        DocumentAmendDetails::getStatementsOfCaseEmpty
    ),
    PROPERTY_DOCUMENTS(
        CaseFileCategory.PROPERTY_DOCUMENTS,
        "propertyDocuments",
        DocumentAmendDetails::getPropertyDocuments,
        DocumentAmendDetails::getPropertyDocumentsEmpty
    ),
    EVIDENCE(
        CaseFileCategory.EVIDENCE,
        "evidence",
        DocumentAmendDetails::getEvidenceDocuments,
        DocumentAmendDetails::getEvidenceEmpty
    ),
    HEARING_DOCUMENTS(
        CaseFileCategory.HEARING_DOCUMENTS,
        "hearingDocuments",
        DocumentAmendDetails::getHearingDocuments,
        DocumentAmendDetails::getHearingDocumentsEmpty
    ),
    ORDERS_AND_NOTICE_OF_HEARINGS(
        CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS,
        "ordersAndNoticeOfHearings",
        DocumentAmendDetails::getOrdersAndNoticeOfHearingsDocuments,
        DocumentAmendDetails::getOrdersAndNoticeOfHearingsEmpty
    ),
    APPLICATIONS(
        CaseFileCategory.APPLICATIONS,
        "applications",
        DocumentAmendDetails::getApplicationsDocuments,
        DocumentAmendDetails::getApplicationsEmpty
    ),
    APPEALS(
        CaseFileCategory.APPEALS,
        "appeals",
        DocumentAmendDetails::getAppealsDocuments,
        DocumentAmendDetails::getAppealsEmpty
    ),
    CORRESPONDENCE(
        CaseFileCategory.CORRESPONDENCE,
        "correspondence",
        DocumentAmendDetails::getCorrespondenceDocuments,
        DocumentAmendDetails::getCorrespondenceEmpty
    ),
    UNCATEGORISED_DOCUMENTS(
        CaseFileCategory.UNCATEGORISED_DOCUMENTS,
        "uncategorisedDocuments",
        DocumentAmendDetails::getUncategorisedDocuments,
        DocumentAmendDetails::getUncategorisedDocumentsEmpty
    );

    final CaseFileCategory category;
    final String idPrefix;
    final TypedPropertyGetter<DocumentAmendDetails, DynamicList> documentsGetter;
    final TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter;

    DocumentCategoryField(CaseFileCategory category,
                          String idPrefix,
                          TypedPropertyGetter<DocumentAmendDetails, DynamicList> documentsGetter,
                          TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter) {
        this.category = category;
        this.idPrefix = idPrefix;
        this.documentsGetter = documentsGetter;
        this.emptyGetter = emptyGetter;
    }
}
