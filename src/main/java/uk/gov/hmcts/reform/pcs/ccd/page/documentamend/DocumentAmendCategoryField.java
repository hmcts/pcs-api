package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;

enum DocumentCategoryField {
    STATEMENTS_OF_CASE(
        CaseFileCategory.STATEMENTS_OF_CASE,
        "StatementsOfCaseDocuments",
        "StatementsOfCaseEmpty",
        PCSCase::getStatementsOfCaseDocuments,
        DocumentAmendDetails::getStatementsOfCaseEmpty
    ),
    PROPERTY_DOCUMENTS(
        CaseFileCategory.PROPERTY_DOCUMENTS,
        "PropertyDocuments",
        "PropertyDocumentsEmpty",
        PCSCase::getPropertyDocuments,
        DocumentAmendDetails::getPropertyDocumentsEmpty
    ),
    EVIDENCE(
        CaseFileCategory.EVIDENCE,
        "EvidenceDocuments",
        "EvidenceEmpty",
        PCSCase::getEvidenceDocuments,
        DocumentAmendDetails::getEvidenceEmpty
    ),
    HEARING_DOCUMENTS(
        CaseFileCategory.HEARING_DOCUMENTS,
        "HearingDocuments",
        "HearingDocumentsEmpty",
        PCSCase::getHearingDocuments,
        DocumentAmendDetails::getHearingDocumentsEmpty
    ),
    ORDERS_AND_NOTICE_OF_HEARINGS(
        CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS,
        "OrdersAndNoticeOfHearingsDocuments",
        "OrdersAndNoticeOfHearingsEmpty",
        PCSCase::getOrdersAndNoticeOfHearingsDocuments,
        DocumentAmendDetails::getOrdersAndNoticeOfHearingsEmpty
    ),
    APPLICATIONS(
        CaseFileCategory.APPLICATIONS,
        "ApplicationsDocuments",
        "ApplicationsEmpty",
        PCSCase::getApplicationsDocuments,
        DocumentAmendDetails::getApplicationsEmpty
    ),
    APPEALS(
        CaseFileCategory.APPEALS,
        "AppealsDocuments",
        "AppealsEmpty",
        PCSCase::getAppealsDocuments,
        DocumentAmendDetails::getAppealsEmpty
    ),
    CORRESPONDENCE(
        CaseFileCategory.CORRESPONDENCE,
        "CorrespondenceDocuments",
        "CorrespondenceEmpty",
        PCSCase::getCorrespondenceDocuments,
        DocumentAmendDetails::getCorrespondenceEmpty
    ),
    UNCATEGORISED_DOCUMENTS(
        CaseFileCategory.UNCATEGORISED_DOCUMENTS,
        "UncategorisedDocuments",
        "UncategorisedDocumentsEmpty",
        PCSCase::getUncategorisedDocuments,
        DocumentAmendDetails::getUncategorisedDocumentsEmpty
    );

    final CaseFileCategory category;
    final String documentsFieldId;
    final String emptyFieldId;
    final TypedPropertyGetter<PCSCase, DynamicList> documentsGetter;
    final TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter;

    DocumentCategoryField(CaseFileCategory category,
                          String documentsFieldId,
                          String emptyFieldId,
                          TypedPropertyGetter<PCSCase, DynamicList> documentsGetter,
                          TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter) {
        this.category = category;
        this.documentsFieldId = documentsFieldId;
        this.emptyFieldId = emptyFieldId;
        this.documentsGetter = documentsGetter;
        this.emptyGetter = emptyGetter;
    }
}
