package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;

/**
 * The enforcement emails to the claimant from the HDPI-7538 notification table, as one call per email.
 *
 * <p>Every method here is safe to call from an event's submit handler. Each one queues the email as a
 * db-scheduler task in the event's transaction, so a rollback drops the email with the case data it describes
 * and a commit keeps both. Nothing here talks to Notify, so a Notify outage cannot fail the event: the task
 * retries on its own. An email with no Notify template written for it yet, or to a claimant with no email
 * address, is logged and skipped; a failure to write the task fails the event.</p>
 *
 * <p>Nothing is sent in any environment until the templates exist. GOV.UK Notify has no API for creating a
 * template, so each one has to be written by hand in the Notify UI and its id configured under
 * {@code enforcement-notify.templates}. {@link EnforcementEmail} lists the templates needed and the
 * personalisation each one has to accept.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementNotificationService {

    /**
     * "Thursday 25 June 2026 at 10:00am", the same wording the eviction confirmation screens use. It is
     * repeated here rather than taken from
     * {@code uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionConfirmationFormat}
     * because the enforcement slices must stay free of cycles and the ccd slice already depends on this one.
     * {@code EnforcementNotificationServiceTest} asserts the two agree.
     */
    private static final DateTimeFormatter DATE_AT_TIME = new DateTimeFormatterBuilder()
        .appendPattern("EEEE d MMMM yyyy 'at' hh:mm")
        .appendText(ChronoField.AMPM_OF_DAY, Map.of(0L, "am", 1L, "pm"))
        .toFormatter(Locale.UK);

    private final EnforcementNotificationSender sender;

    /**
     * Tells the claimant their application has been received and is waiting for a caseworker's review. Call it
     * when the application reaches awaiting review: when the fee is paid, or on submit for a type with no fee.
     */
    public void notifyClaimantOfApplicationReceived(long caseReference, SelectEnforcementType enforcementType) {
        queue(caseReference, EnforcementEmail.forApplicationReceived(enforcementType), Map.of());
    }

    /**
     * Tells the claimant the caseworker approved their application, with the warrant or writ number.
     *
     * <p>Only a warrant and a writ of possession have an approval email: the restitution wording is TBC
     * (OPEN-QUESTIONS 17), so nothing is sent for those types.</p>
     */
    public void notifyClaimantOfApproval(long caseReference, SelectEnforcementType enforcementType,
                                         String enforcementNumber) {
        queue(caseReference, EnforcementEmail.forApproval(enforcementType),
                          Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, nullToEmpty(enforcementNumber)));
    }

    /**
     * Tells the claimant the caseworker rejected their application, with the caseworker's reason.
     *
     * <p>Only a warrant of possession has a rejection email: the writ and restitution wording is TBC
     * (OPEN-QUESTIONS 17), so nothing is sent for those types.</p>
     */
    public void notifyClaimantOfRejection(long caseReference, SelectEnforcementType enforcementType,
                                          String rejectionReason) {
        queue(caseReference, EnforcementEmail.forRejection(enforcementType),
                          Map.of(EnforcementEmail.REJECTION_REASON, nullToEmpty(rejectionReason)));
    }

    /**
     * Asks the claimant to confirm the eviction date the bailiff has just scheduled.
     */
    public void notifyClaimantOfScheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.EVICTION_SCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Asks the claimant to confirm the new eviction date the bailiff has just rescheduled to.
     */
    public void notifyClaimantOfRescheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.EVICTION_RESCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Tells the claimant they have confirmed they can attend the eviction on this date.
     */
    public void notifyClaimantOfConfirmedAttendance(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Tells the claimant the court has their answer that they cannot attend the eviction.
     */
    public void notifyClaimantOfDeclinedAttendance(long caseReference) {
        queue(caseReference, EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE, Map.of());
    }

    /**
     * The date as the confirmation screens show it, so the email and the screen agree: "Thursday 25 June 2026
     * at 10:00am".
     */
    private static Map<String, Object> evictionPersonalisation(LocalDateTime evictionDateTime) {
        String formatted = evictionDateTime == null ? "" : DATE_AT_TIME.format(evictionDateTime);
        return Map.of(EnforcementEmail.EVICTION_DATE_TIME, formatted);
    }

    private void queue(long caseReference, EnforcementEmail email, Map<String, Object> personalisation) {
        if (email == null) {
            log.info("No enforcement email has been specified for this outcome on case {}", caseReference);
            return;
        }
        sender.send(caseReference, email, personalisation);
    }

    // Notify rejects a null personalisation value, so send an empty placeholder rather than failing the email.
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
