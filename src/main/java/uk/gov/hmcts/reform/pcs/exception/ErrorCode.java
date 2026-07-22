package uk.gov.hmcts.reform.pcs.exception;

/**
 * An error code taxonomy where by the 'internalCode' is used to identify what has happened.  Then the system needs to
 *  be set accordingly to identify further details on these issues.   This is to prevent leaking PII into logs and
 *  beyond the server itself to the front end or other actors.
 */
public enum ErrorCode {

    DOC_ASSEMBLY_NO_URL_RETURNED("DOC_ASSEMBLY_1",
                                 "No document URL returned from Doc Assembly service"),
    DOC_GENERATION_FAILED("DOC_GENERATION_1",
                          "Document generation failed"),
    DOC_GENERATION_UNEXPECTED_ERROR("DOC_GENERATION_2",
                          "Unexpected error occurred during document generation"),
    ACCESS_CODE_ALREADY_IN_USE("ACCESS_CODE",
                               "This access code is already linked to a user."),
    BULK_PRINT_MERGE_ERROR("BULK_PRINT_MERGE", "Failed to merge bulk-print PDFs"),
    META_DATA_FOR_DOCUMENT_ERROR("META_DATA_ERROR",
                                 "Failed to retrieve document metadata from CDAM"),
    MISSING_POSTAL_ADDRESS("MISSING_POSTAL_ADDRESS", "No postal address for party"),
    DEFENDANT_ACCESS_VALIDATOR("DEFENDANT_ACCESS_VALIDATOR",
                               "User is not linked as a defendant on this case"),
    DEFENDANT_PARTY_EXTRACTOR("DEFENDANT_PARTY_EXTRACTOR_01", "No claim found for this case"),
    DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS("DEFENDANT_PARTY_EXTRACTOR_02",
                                            "No defendants associated with this case"),
    LEGAL_REP_FOR_DEFENDANT_ACCESS("LEGAL_REP_FOR_DEFENDANT_ACCESS",
                                   "User is not linked as a defendant solicitor on this case"),
    LEGAL_REP_PARTY_SELECTION("LEGAL_REP_PARTY_SELECTION",
                              "User is not linked as a defendant on this case"),
    CASE_NOT_FOUND("CASE_NOT_FOUND","No case found with reference"),
    CLAIM_NOT_FOUND("CLAIM_NOT_FOUND","No claim found for case reference"),
    FEE_PAYMENT("FEE_PAYMENT_01", "No fee payment entity found"),
    FEE_PAYMENT_NOTIFICATION("FEE_PAYMENT_02", "Fee payment not found"),
    GEN_APP("GEN_APP", "Statement of truth must be accepted to create a gen app"),
    GEN_APP_NOT_FOUND("GEN_APP_NOT_FOUND", "Unable to find gen app"),
    POST_CODE("POST_CODE","Postcode can't be empty or null"),
    SECURITY_CONTEXT("SECURITY_CONTEXT", "No authentication instance found"),
    AUTHENTICATION_PRINCIPAL("AUTHENTICATION PRINCIPLE",
                             "Authentication principal is null or not of the expected type"),
    TEMPLATE_RENDERING("TEMPLATE_RENDERING", "Failed to render template"),
    UNSUBMITTED_DATA("UNSUBMITTED_DATA_01", "Failed to save answers"),
    UPDATE_DRAFT("UNSUBMITTED_DATA_02", "Failed to update draft case data"),
    NO_UNSUBMITTED_CASE_DATA("UNSUBMITTED_DATA_04",
                             "No unsubmitted case data found for case"),
    READ_FAIL("UNSUBMITTED_DATA_05", "Failed to read saved answers"),
    EMAIL_FAILED_TO_SEND("EMAIL_FAILED_TO_SEND",
                         "Email failed to send, please try again."),
    FETCH_NOTIFICATION_FAIL("FETCH_NOTIFICATION_FAIL",
                            "Failed to fetch notification, please try again."),
    FAILED_SAVE_CASE("FAILED_SAVE", "Failed to save Case Notification."),
    NOTIFICATION_ERROR("NOTIFICATION_ERROR", "Null notification ID from email service"),
    TEMP_EMAIL_SEND("EMAIL_SEND", "Email temporarily failed to send."),
    FEE_TASK_DATA_ISSUE("FEE_TASK_DATA_ISSUE",
                        "Unable to write to json the FeesAndPayTaskData"),
    COUNTER_CLAIM_CALLBACK("COUNTER_CLAIM_CALLBACK",
                           "Counterclaim payment callback missing relatedEntityId in task data"),
    COUNTER_CLAIM_TASK_DATA("COUNTER_CLAIM_TASK_DATA", "Unable to process task data"),
    CALLBACK_TASK_DATA("CALL_BACK_TASK_DATA", "Unable to process task data"),
    FEE_NOT_FOUND("FEE_NOT_FOUND", "Fee not found"),
    UNABLE_TO_RETRIEVE_FEE("UNABLE_TO_RETRIEVE_FEE", "Unable to retrieve fee"),
    UNEXPECTED_ELIGIBILITY("UNEXPECTED_ELIGIBILITY", "Unexpected eligibility status"),
    LEGISLATIVE_COUNTRY_REQUIREMENT("LEGISLATIVE_COUNTRY_REQUIREMENT",
                                    "Expected at least 2 legislative countries"),
    MIGRATION_NOT_YET_APPLIED("MIGRATION_NOT_YET_APPLIED",
                              "Found migration not yet applied"),
    PARTY_BY_ENTITY_AND_CASE("PARTY_BY_ENTITY_AND_CASE", "No party found"),
    PARTY_BY_IDAM_AND_CASE("PARTY_BY_IDAM_AND_CASE", "No party found"),
    PARTY_TYPE("PARTY_TYPE", "No Party of type"),
    PARTY_NOT_FOUND("PARTY_NOT_FOUND", "Party not found"),
    AUTH_VALIDATION("IDAM_VALIDATION", "Unable to validate authorization token"),
    AUTH_TOKEN_EMPTY("IDAM_EMPTY_TOKEN", "Unable to get access token response"),
    AUTH_TOKEN_RETRIEVAL_FAIL("AUTH_TOKEN_RETRIEVAL_FAIL",
                              "Unable to get access token response"),
    AUTH_BLANK("AUTH_BLANK", "Authorization token is null or blank"),
    AUTH_MALFORMED("AUTH_MALFORMED", "Malformed Authorization token"),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED",
                      "The Authorization token provided is expired or invalid"),
    ACCESS_CODE_ISSUE("ACCESS_CODE_ISSUE", "Invalid data"),
    ORGANISATION_DETAILS("ORGANISATION_DETAILS",
                         "Failed to retrieve organisation details"),
    PARTY_LINK_EXISTS("PARTY_LINK_EXISTS", "Already linked to Party"),
    PARTY_ACCESS_CODE("PARTY_ACCESS_CODE",
                      "The party this access code was generated for is not a defendant in this case"),
    DOCUMENT_DOWNLOAD("DOCUMENT_DOWNLOAD", "Document download issue"),
    DRAFT_NOT_FOUND("DRAFT_NOT_FOUND", "No draft found"),

    REMOTE_CALL("REMOTE_CALL", "Remote call"),

    TEST_CASE_SUPPORT("", "");

    private final String internalCode;
    private final String safeDescription;

    ErrorCode(String internalCode, String safeDescription) {
        this.internalCode = internalCode;
        this.safeDescription = safeDescription;
    }

    public String internalCode() {
        return internalCode;
    }

    public String safeDescription() {
        return safeDescription;
    }

}
