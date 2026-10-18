package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.Getter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.List;

/**
 * Every claimant email the enforcement journeys send, named as the HDPI-7538 notification table names it.
 *
 * <p>Each one is a separate Notify template. A Notify template can only be written by a person signed in to
 * the Notify web UI — there is no API that creates one — so pcs-api holds no body text for these emails. It
 * supplies the template id and the personalisation, and Notify holds the content.</p>
 *
 * <p>{@link #getTemplateKey()} is the key under {@code enforcement-notify.templates} that carries the
 * template id. No template has been written for any of these yet, so every key is blank in every environment
 * and {@link EnforcementNotificationSender} sends nothing until one is set.</p>
 *
 * <p>{@link #getPersonalisation()} lists the personalisation this email needs on top of the
 * {@link uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation} fields every PCS email
 * gets ({@code firstName}, {@code lastName}, {@code caseNumber}, {@code claimantName},
 * {@code primaryDefendantName}). Whoever writes the template in Notify needs exactly these placeholders.</p>
 */
@Getter
public enum EnforcementEmail {

    /**
     * 01-claimant-applies-for-enforcement.feature, "Claimant is notified that the application was received". Sent
     * when the application reaches awaiting review: once the fee is paid, or on submit for a type with no fee.
     * HDPI-7538 gives the warrant and writ of possession wording; the restitution wording is still TBC
     * (OPEN-QUESTIONS 55).
     */
    WARRANT_APPLICATION_RECEIVED("Request for a warrant received", "warrant-application-received"),
    WRIT_APPLICATION_RECEIVED("Request for a writ received", "writ-application-received"),
    WARRANT_OF_RESTITUTION_APPLICATION_RECEIVED(
        "Warrant of restitution application submitted", "warrant-of-restitution-application-received"),
    WRIT_OF_RESTITUTION_APPLICATION_RECEIVED(
        "Writ of restitution application submitted", "writ-of-restitution-application-received"),

    /**
     * 02-caseworker-decides-enforcement.feature, "Approving a warrant of possession generates the sealed
     * warrant" and "Approving a writ of possession generates the sealed writ for the claimant".
     */
    WARRANT_APPROVED("Caseworker generates the warrant of possession", "warrant-approved", "enforcementNumber"),
    WRIT_APPROVED("Application for a writ successful", "writ-approved", "enforcementNumber"),

    /**
     * 02-caseworker-decides-enforcement.feature, "Rejecting an application".
     */
    APPLICATION_REJECTED(
        "Caseworker rejects the warrant of possession application", "application-rejected", "rejectionReason"),

    /**
     * 05-schedule-eviction.feature and 07-reschedule-eviction.feature, "Claimant is asked to confirm the
     * eviction date" / "the new date". Both ask the claimant to confirm, so both templates need a link to the
     * case in XUI. pcs-api has no XUI base URL configured, so the link has to be written into the Notify
     * template itself rather than passed as personalisation.
     */
    EVICTION_SCHEDULED("Bailiff schedules the eviction date", "eviction-scheduled", "evictionDateTime"),
    EVICTION_RESCHEDULED("Bailiff reschedules the eviction date", "eviction-rescheduled", "evictionDateTime"),

    /**
     * 06-claimant-confirms-eviction.feature, "Claimant confirms they can attend" and "Claimant cannot attend".
     * Sent by {@code ClaimantEvictionConfirmationService} when the answer moves the warrant on. HDPI-7538 has the
     * wording for the confirmation only; the "declines attendance" wording is TBC (OPEN-QUESTIONS 55).
     */
    CLAIMANT_CONFIRMED_ATTENDANCE(
        "Claimant confirms attendance at the eviction date", "claimant-confirmed-attendance", "evictionDateTime"),
    CLAIMANT_DECLINED_ATTENDANCE(
        "Claimant declines attendance at the eviction date", "claimant-declined-attendance"),

    /**
     * 11-record-warrant-return.feature, "Claimant is notified of the return". One template covers every
     * return name that notifies the claimant: the body varies too much to be worth a template each, so
     * {@link WarrantReturnClaimantEmails} holds the bodies and passes the right one as personalisation.
     */
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

    /**
     * The email telling the claimant their application has been received, or null for an unknown type. HDPI-7538
     * names one for every type, so unlike approval and rejection no type is left out here: a type whose wording
     * is TBC is held back by leaving its template id unset.
     */
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

    /**
     * The email telling the claimant their application was approved, or null for a type whose content the
     * service has not written yet. HDPI-7538 names an approval email for a warrant and for a writ of
     * possession only; the restitution wording is TBC (OPEN-QUESTIONS 56), and nothing is sent until it
     * exists.
     */
    public static EnforcementEmail forApproval(SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            return null;
        }
        return switch (enforcementType) {
            case WARRANT -> WARRANT_APPROVED;
            case WRIT -> WRIT_APPROVED;
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> null;
        };
    }

    /**
     * The email telling the claimant their application was rejected, or null for a type whose content the
     * service has not written yet. HDPI-7538 names a rejection email for a warrant of possession only; the
     * writ and restitution wording is TBC (OPEN-QUESTIONS 56).
     */
    public static EnforcementEmail forRejection(SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            return null;
        }
        return switch (enforcementType) {
            case WARRANT -> APPLICATION_REJECTED;
            case WRIT, WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> null;
        };
    }
}
