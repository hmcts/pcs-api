package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.Getter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.List;

@Getter
public enum EnforcementEmail {

    WARRANT_APPLICATION_RECEIVED("Request for a warrant received", "warrant-application-received"),
    WRIT_APPLICATION_RECEIVED("Request for a writ received", "writ-application-received"),
    WARRANT_OF_RESTITUTION_APPLICATION_RECEIVED(
        "Warrant of restitution application submitted", "warrant-of-restitution-application-received"),
    WRIT_OF_RESTITUTION_APPLICATION_RECEIVED(
        "Writ of restitution application submitted", "writ-of-restitution-application-received"),

    WARRANT_APPROVED("Caseworker generates the warrant of possession", "warrant-approved", "enforcementNumber"),
    WRIT_APPROVED("Application for a writ successful", "writ-approved", "enforcementNumber"),
    WARRANT_OF_RESTITUTION_APPROVED(
        "Caseworker generates the warrant of restitution", "warrant-of-restitution-approved"),

    WARRANT_REJECTED(
        "Caseworker rejects the warrant of possession application", "warrant-rejected", "rejectionReason"),
    WRIT_REJECTED("Caseworker rejects the writ of possession application", "writ-rejected"),
    WARRANT_OF_RESTITUTION_REJECTED(
        "Caseworker rejects the warrant of restitution application", "warrant-of-restitution-rejected"),

    EVICTION_SCHEDULED("Bailiff schedules the eviction date", "eviction-scheduled", "evictionDateTime"),
    EVICTION_RESCHEDULED("Bailiff reschedules the eviction date", "eviction-rescheduled", "evictionDateTime"),

    CLAIMANT_CONFIRMED_ATTENDANCE(
        "Claimant confirms attendance at the eviction date", "claimant-confirmed-attendance", "evictionDateTime"),
    CLAIMANT_DECLINED_ATTENDANCE(
        "Claimant declines attendance at the eviction date", "claimant-declined-attendance"),

    WARRANT_RETURN("Claimant is notified of the return", "warrant-return", "returnName", "warrantReturnBody");

    public static final String ENFORCEMENT_NUMBER = "enforcementNumber";
    public static final String REJECTION_REASON = "rejectionReason";
    public static final String EVICTION_DATE_TIME = "evictionDateTime";
    public static final String RETURN_NAME = "returnName";
    public static final String WARRANT_RETURN_BODY = "warrantReturnBody";

    private final String templateName;
    private final String templateKey;
    private final List<String> personalisation;

    EnforcementEmail(String templateName, String templateKey, String... personalisation) {
        this.templateName = templateName;
        this.templateKey = templateKey;
        this.personalisation = List.of(personalisation);
    }

    public static EnforcementEmail forApplicationReceived(SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            return null;
        }
        return switch (enforcementType) {
            case WARRANT -> WARRANT_APPLICATION_RECEIVED;
            case WRIT -> WRIT_APPLICATION_RECEIVED;
            case WARRANT_OF_RESTITUTION -> WARRANT_OF_RESTITUTION_APPLICATION_RECEIVED;
            case WRIT_OF_RESTITUTION -> WRIT_OF_RESTITUTION_APPLICATION_RECEIVED;
        };
    }

    public static EnforcementEmail forApproval(SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            return null;
        }
        return switch (enforcementType) {
            case WARRANT -> WARRANT_APPROVED;
            case WRIT -> WRIT_APPROVED;
            case WARRANT_OF_RESTITUTION -> WARRANT_OF_RESTITUTION_APPROVED;
            case WRIT_OF_RESTITUTION -> null;
        };
    }

    public static EnforcementEmail forRejection(SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            return null;
        }
        return switch (enforcementType) {
            case WARRANT -> WARRANT_REJECTED;
            case WRIT -> WRIT_REJECTED;
            case WARRANT_OF_RESTITUTION -> WARRANT_OF_RESTITUTION_REJECTED;
            case WRIT_OF_RESTITUTION -> null;
        };
    }
}
