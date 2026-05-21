package uk.gov.hmcts.reform.pcs.ccd.event;

public enum EventId {

    createPossessionClaim,
    resumePossessionClaim,
    enforceTheOrder,
    respondPossessionClaim,
    submitDefendantResponse,
    citizenCreateGenApp,
    createTestCase,
    createCaseLink,
    maintainCaseLink,
    dashboardView,
    confirmEviction

    ;

    public String externalId() {
        return switch (this) {
            case createPossessionClaim -> "ext:create";
            case resumePossessionClaim -> "ext:resume";
            case enforceTheOrder -> "ext:enforce";
            default -> "ext:" + name();
        };
    }
}
