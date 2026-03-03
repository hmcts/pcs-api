package uk.gov.hmcts.reform.pcs.taskmanagement;

public enum WorkType {
    ROUTINE_WORK,
    PRIORITY,
    HEARING_WORK,
    DECISION_MAKING_WORK,
    APPLICATIONS;

    public String getLowerCaseName() {
        return name().toLowerCase();
    }

}




