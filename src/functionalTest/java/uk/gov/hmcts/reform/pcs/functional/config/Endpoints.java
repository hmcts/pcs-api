package uk.gov.hmcts.reform.pcs.functional.config;

public enum Endpoints {

    DashboardNotifications("/dashboard/{caseReference}/notifications"),
    DashboardTasks("/dashboard/{caseReference}/tasks"),
    ClaimEligibility("/testing-support/claim-eligibility"),
    DeleteTestCase("/testing-support/cases/{caseReference}"),
    ValidateAccessCode("/cases/{caseReference}/validate-access-code");

    private final String resource;

    Endpoints(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
