package uk.gov.hmcts.reform.pcs.reference.commondata;

import lombok.Getter;

@Getter
public enum ReferenceDataSource {

    JUDGE_TYPE("judge_type", "JudgeType"),
    HEARING_CHANNEL("hearing_channel", "HearingChannel"),
    HEARING_PRIORITY("hearing_priority", "HearingPriority"),
    HEARING_CASE_SUBTYPE("hearing_case_subtype", "caseSubType"),
    FACILITIES("facilities", "Facilities"),
    AUTO_LIST_CHANGE_REASONS("auto_list_change_reasons", "AutoListChangeReasons"),
    ADDITIONAL_ROLES("additional_roles", "AdditionalRoles"),
    ACTUAL_PART_HEARD_REASON("actual_part_heard_reason", "ActualPartHeardReasonCodes"),
    CHANGE_REASONS("change_reasons", "ChangeReasons"),
    LISTING_STATUS("listing_status", "ListingStatus"),
    ACTUAL_CANCELLATION_REASON("actual_cancellation_reason", "ActualCancellationReasonCodes"),
    CASE_MANAGEMENT_CANCELLATION_REASON("case_management_cancellation_reason", "CaseManagementCancellationReasons"),
    HEARING_TYPE("hearing_type", "HearingType"),
    SIGN_LANGUAGE("sign_language", "SignLanguage"),
    UNAVAILABLE_TYPE("unavailable_type", "UnavailableType"),
    HEARING_CASE_TYPE("hearing_case_type", "caseType"),
    INTERPRETER_LANGUAGE("interpreter_language", "InterpreterLanguage"),
    HEARING_SUBCHANNEL("hearing_subchannel", "HearingSubChannel"),
    CUSTODY_STATUS("custody_status", "CustodyStatus"),
    PARTY_RELATIONSHIP_TYPE("party_relationship_type", "PartyRelationshipType");

    private final String tableName;
    private final String categoryId;

    ReferenceDataSource(String tableName, String categoryId) {
        this.tableName = tableName;
        this.categoryId = categoryId;
    }
}
