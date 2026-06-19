package uk.gov.hmcts.reform.pcs.ccd.event;

public enum EventId {

    createPossessionClaim,
    resumePossessionClaim,
    enforceTheOrder,
    respondPossessionClaim,
    submitDefendantResponse,
    makeAnApplication,
    createTestCase,
    createCaseLink,
    maintainCaseLink,
    dashboardView,
    confirmEviction,
    addCaseNote,
    createFlags,
    amendFlags,
    claimIssuePayment,
    DELETE_DRAFT_CLAIM("deleteDraftClaim");

    private final String id;

    EventId() {
        this.id = name();
    }

    EventId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
