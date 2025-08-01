package uk.gov.hmcts.reform.pcs.functional.config;

public enum Endpoints {

    DashboardNotifications("/dashboard/{caseReference}/notifications"),
    DashboardTasks("/dashboard/{caseReference}/tasks"),
    Courts("/courts"),
    ClaimEligibility("/testing-support/claim-eligibility");

    private final String resource;

    Endpoints(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
