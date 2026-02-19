package uk.gov.hmcts.reform.pcs.functional.config;

public enum Endpoints {

    DashboardNotifications("/dashboard/{caseReference}/notifications"),
    DashboardTasks("/dashboard/{caseReference}/tasks"),
    ClaimEligibility("/testing-support/claim-eligibility"),
    CreateTestCase("/testing-support/{legislativeCountry}/create-case"),
    GetPins("/testing-support/pins/{caseReference}"),
    ValidateAccessCode("/cases/{caseReference}/validate-access-code"),
    StartEventCallback("/callbacks/about-to-start"),
    SubmitEventCallback("/ccd-persistence/cases");

    private final String resource;

    Endpoints(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
