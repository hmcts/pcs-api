package uk.gov.hmcts.reform.pcs.functional.config;

public enum Endpoints {

    DashboardNotifications("/dashboard/{caseReference}/notifications"),
    DashboardTasks("/dashboard/{caseReference}/tasks"),
    ClaimEligibility("/testing-support/claim-eligibility"),
    CreateTestCase("/testing-support/create-case"),
    DeleteTestCase("/testing-support/cases/{caseReference}");

    private final String resource;

    Endpoints(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
