package uk.gov.hmcts.reform.pcs.taskmanagement;

public final class TaskConstants {

    public static final String JURISDICTION = "ST_CIC";
    public static final String DEFAULT_REGION = "1";
    public static final String DEFAULT_LOCATION = "336559";
    public static final String DEFAULT_EXECUTION_TYPE = "Case Management Task";
    public static final String DEFAULT_SECURITY_CLASSIFICATION = "PUBLIC";
    public static final String DEFAULT_TASK_SYSTEM = "SELF";
    public static final int MAJOR_PRIORITY = 5000;
    public static final int MINOR_PRIORITY = 500;

    public static final String GLASGOW_TRIBUNALS_CENTRE = "Glasgow Tribunals Centre";
    public static final String CRIMINAL_INJURIES_COMPENSATION = "Criminal Injuries Compensation";
    public static final String CIC_CASE_TYPE = "CriminalInjuriesCompensation";

    public static final String EMPTY_DESCRIPTION = "";
    public static final String SEND_ORDER_DESCRIPTION =
        "[Orders: Send order](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-send-order)";
    public static final String ISSUE_DECISION_DESCRIPTION =
        "[Decision: Issue a decision](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-issue-decision)"
            + "<br/>"
            + "[Decision: Issue final decision]"
            + "(/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-issue-final-decision)";
    public static final String COMPLETE_HEARING_OUTCOME_DESCRIPTION =
        "[Hearings:Create summary](/cases/case-details/${[CASE_REFERENCE]}/trigger/create-hearing-summary)";
    public static final String ISSUE_CASE_TO_RESPONDENT_DESCRIPTION =
        "[Case: Issue to respondent](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-issue-case)";
    public static final String VET_NEW_CASE_DOCUMENTS_DESCRIPTION =
        "[Case: Build case](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-case-built)";
    public static final String CREATE_DUE_DATE_DESCRIPTION =
        "[Orders: Create draft](/cases/case-details/${[CASE_REFERENCE]}/trigger/create-draft-order)";
    public static final String REVIEW_REQUESTS_DESCRIPTION =
        "[Orders: Create and send order](/cases/case-details/${[CASE_REFERENCE]}/trigger/create-and-send-order)";
    public static final String FOLLOW_UP_NONCOMPLIANCE_DESCRIPTION =
        "[Document management: Upload](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-document-management)"
            + "<br/>"
            + "[Orders: Manage due date](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-amend-due-date)"
            + "<br/>"
            + "[Refer case to judge](/cases/case-details/${[CASE_REFERENCE]}/trigger/refer-to-judge)"
            + "<br/>"
            + "[Refer case to legal officer]"
            + "(/cases/case-details/${[CASE_REFERENCE]}/trigger/refer-to-legal-officer)"
            + "<br/>"
            + "[Case: Contact parties](/cases/case-details/${[CASE_REFERENCE]}/trigger/contact-parties)";
    public static final String REGISTER_NEW_CASE_DESCRIPTION =
        "[Case: Edit case](/cases/case-details/${[CASE_REFERENCE]}/trigger/edit-case)";
    public static final String STITCH_COLLATE_BUNDLE_DESCRIPTION =
        "[Bundle: Create a bundle](/cases/case-details/${[CASE_REFERENCE]}/trigger/createBundle)";
    public static final String PROCESS_FURTHER_EVIDENCE_DESCRIPTION =
        "[Document management: Amend](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-amend-document)"
            + "<br/>"
            + "[Case: Edit case](/cases/case-details/${[CASE_REFERENCE]}/trigger/edit-case)"
            + "<br/>"
            + "[Refer case to judge](/cases/case-details/${[CASE_REFERENCE]}/trigger/refer-to-judge)"
            + "<br/>"
            + "[Refer case to legal officer]"
            + "(/cases/case-details/${[CASE_REFERENCE]}/trigger/refer-to-legal-officer)"
            + "<br/>"
            + "[Case: Contact parties](/cases/case-details/${[CASE_REFERENCE]}/trigger/contact-parties)";

    public static final String RULE_27_DESCRIPTION =
        "[Orders: Send order](/cases/case-details/${[CASE_REFERENCE]}/trigger/caseworker-send-order)";


    private TaskConstants() {
    }
}


