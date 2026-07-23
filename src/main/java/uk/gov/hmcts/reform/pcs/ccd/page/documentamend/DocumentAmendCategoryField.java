package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;

enum DocumentAmendCategoryField {
    STATEMENTS_OF_CASE(
        CaseFileCategory.STATEMENTS_OF_CASE,
        "statementsOfCase",
        PCSCase::getStatementsOfCaseDocuments,
        DocumentAmendDetails::getStatementsOfCaseEmpty
    ),
    PROPERTY_DOCUMENTS(
        CaseFileCategory.PROPERTY_DOCUMENTS,
        "propertyDocuments",
        PCSCase::getPropertyDocuments,
        DocumentAmendDetails::getPropertyDocumentsEmpty
    ),
    EVIDENCE(
        CaseFileCategory.EVIDENCE,
        "evidence",
        PCSCase::getEvidenceDocuments,
        DocumentAmendDetails::getEvidenceEmpty
    ),
    HEARING_DOCUMENTS(
        CaseFileCategory.HEARING_DOCUMENTS,
        "hearingDocuments",
        PCSCase::getHearingDocuments,
        DocumentAmendDetails::getHearingDocumentsEmpty
    ),
    ORDERS_AND_NOTICE_OF_HEARINGS(
        CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS,
        "ordersAndNoticeOfHearings",
        PCSCase::getOrdersAndNoticeOfHearingsDocuments,
        DocumentAmendDetails::getOrdersAndNoticeOfHearingsEmpty
    ),
    APPLICATIONS(
        CaseFileCategory.APPLICATIONS,
        "applications",
        PCSCase::getApplicationsDocuments,
        DocumentAmendDetails::getApplicationsEmpty
    ),
    APPEALS(
        CaseFileCategory.APPEALS,
        "appeals",
        PCSCase::getAppealsDocuments,
        DocumentAmendDetails::getAppealsEmpty
    ),
    CORRESPONDENCE(
        CaseFileCategory.CORRESPONDENCE,
        "correspondence",
        PCSCase::getCorrespondenceDocuments,
        DocumentAmendDetails::getCorrespondenceEmpty
    ),
    UNCATEGORISED_DOCUMENTS(
        CaseFileCategory.UNCATEGORISED_DOCUMENTS,
        "uncategorisedDocuments",
        PCSCase::getUncategorisedDocuments,
        DocumentAmendDetails::getUncategorisedDocumentsEmpty
    );

    final CaseFileCategory category;
    final String idPrefix;
    final TypedPropertyGetter<PCSCase, DynamicList> documentsGetter;
    final TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter;

    DocumentAmendCategoryField(CaseFileCategory category,
                          String idPrefix,
                          TypedPropertyGetter<PCSCase, DynamicList> documentsGetter,
                          TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter) {
        this.category = category;
        this.idPrefix = idPrefix;
        this.documentsGetter = documentsGetter;
        this.emptyGetter = emptyGetter;
    }
}
