package uk.gov.hmcts.reform.pcs.functional.config;

public enum Endpoints {

    DashboardNotifications("/dashboard/{caseReference}/notifications"),
    DashboardTasks("/dashboard/{caseReference}/tasks");

    private final String resource;

    Endpoints(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
