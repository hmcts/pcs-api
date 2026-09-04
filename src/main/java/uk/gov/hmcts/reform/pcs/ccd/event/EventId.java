package uk.gov.hmcts.reform.pcs.ccd.event;

@SuppressWarnings("java:S115") // constant names match the camelCase CCD event IDs registered via .name()
public enum EventId {

    createPossessionClaim,
    resumePossessionClaim,
    enforceTheOrder,
    respondPossessionClaim,
    submitDefendantResponse,
    makeAnApplication,
    claimantMakeAnApplication,
    createTestCase,
    createCaseLink,
    maintainCaseLink,
    dashboardView,
    confirmEviction,
    uploadDocuments,
    amendDocuments,
    addCaseNote,
    addCaseReviewDate,
    createFlags,
    amendFlags,
    requestSupport,
    manageSupport,
    claimIssuePayment,
    changeCaseState,
    manageHearing,
    enterGenApp,
    caseworkerUploadDocuments,
    removeDocument,
    legalRepDocumentUpload,
    manageParties,
    defendantPaperResponse,
    legalRepresentativeContactDetails,
    noticeOfChangeApplied
}
