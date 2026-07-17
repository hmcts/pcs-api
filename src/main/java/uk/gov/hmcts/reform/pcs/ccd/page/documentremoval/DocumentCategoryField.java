package uk.gov.hmcts.reform.pcs.ccd.page.documentremoval;

import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentremoval.DocumentRemovalDetails;

enum DocumentCategoryField {
    STATEMENTS_OF_CASE(
        CaseFileCategory.STATEMENTS_OF_CASE,
        "StatementsOfCaseDocuments",
        "StatementsOfCaseEmpty",
        PCSCase::getStatementsOfCaseDocuments,
        DocumentRemovalDetails::getStatementsOfCaseEmpty,
        DocumentRemovalDetails::getStatementsOfCaseReason
    ),
    PROPERTY_DOCUMENTS(
        CaseFileCategory.PROPERTY_DOCUMENTS,
        "PropertyDocuments",
        "PropertyDocumentsEmpty",
        PCSCase::getPropertyDocuments,
        DocumentRemovalDetails::getPropertyDocumentsEmpty,
        DocumentRemovalDetails::getPropertyDocumentsReason
    ),
    EVIDENCE(
        CaseFileCategory.EVIDENCE,
        "EvidenceDocuments",
        "EvidenceEmpty",
        PCSCase::getEvidenceDocuments,
        DocumentRemovalDetails::getEvidenceEmpty,
        DocumentRemovalDetails::getEvidenceReason
    ),
    HEARING_DOCUMENTS(
        CaseFileCategory.HEARING_DOCUMENTS,
        "HearingDocuments",
        "HearingDocumentsEmpty",
        PCSCase::getHearingDocuments,
        DocumentRemovalDetails::getHearingDocumentsEmpty,
        DocumentRemovalDetails::getHearingDocumentsReason
    ),
    ORDERS_AND_NOTICE_OF_HEARINGS(
        CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS,
        "OrdersAndNoticeOfHearingsDocuments",
        "OrdersAndNoticeOfHearingsEmpty",
        PCSCase::getOrdersAndNoticeOfHearingsDocuments,
        DocumentRemovalDetails::getOrdersAndNoticeOfHearingsEmpty,
        DocumentRemovalDetails::getOrdersAndNoticeOfHearingsReason
    ),
    APPLICATIONS(
        CaseFileCategory.APPLICATIONS,
        "ApplicationsDocuments",
        "ApplicationsEmpty",
        PCSCase::getApplicationsDocuments,
        DocumentRemovalDetails::getApplicationsEmpty,
        DocumentRemovalDetails::getApplicationsReason
    ),
    APPEALS(
        CaseFileCategory.APPEALS,
        "AppealsDocuments",
        "AppealsEmpty",
        PCSCase::getAppealsDocuments,
        DocumentRemovalDetails::getAppealsEmpty,
        DocumentRemovalDetails::getAppealsReason
    ),
    CORRESPONDENCE(
        CaseFileCategory.CORRESPONDENCE,
        "CorrespondenceDocuments",
        "CorrespondenceEmpty",
        PCSCase::getCorrespondenceDocuments,
        DocumentRemovalDetails::getCorrespondenceEmpty,
        DocumentRemovalDetails::getCorrespondenceReason
    ),
    UNCATEGORISED_DOCUMENTS(
        CaseFileCategory.UNCATEGORISED_DOCUMENTS,
        "UncategorisedDocuments",
        "UncategorisedDocumentsEmpty",
        PCSCase::getUncategorisedDocuments,
        DocumentRemovalDetails::getUncategorisedDocumentsEmpty,
        DocumentRemovalDetails::getUncategorisedDocumentsReason
    );

    final CaseFileCategory category;
    final String documentsFieldId;
    final String emptyFieldId;
    final TypedPropertyGetter<PCSCase, DynamicList> documentsGetter;
    final TypedPropertyGetter<DocumentRemovalDetails, YesOrNo> emptyGetter;
    final TypedPropertyGetter<DocumentRemovalDetails, String> reasonGetter;

    DocumentCategoryField(CaseFileCategory category,
                          String documentsFieldId,
                          String emptyFieldId,
                          TypedPropertyGetter<PCSCase, DynamicList> documentsGetter,
                          TypedPropertyGetter<DocumentRemovalDetails, YesOrNo> emptyGetter,
                          TypedPropertyGetter<DocumentRemovalDetails, String> reasonGetter) {
        this.category = category;
        this.documentsFieldId = documentsFieldId;
        this.emptyFieldId = emptyFieldId;
        this.documentsGetter = documentsGetter;
        this.emptyGetter = emptyGetter;
        this.reasonGetter = reasonGetter;
    }
}
