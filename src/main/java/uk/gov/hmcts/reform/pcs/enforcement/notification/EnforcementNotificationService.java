package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.util.EvictionDateFormat;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.util.AfterCommit;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The enforcement emails to the claimant from the HDPI-7538 notification table, as one call per email.
 *
 * <p>Every method here is safe to call from an event's submit handler. Each one defers the send until the
 * event's transaction has committed, so the claimant is never told about case data that is then rolled back,
 * and none of them throws: an email that cannot be sent — because no Notify template has been written for it
 * yet, or the claimant has no email address, or Notify is unreachable — is logged and the event still
 * succeeds.</p>
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

    private final EnforcementNotificationSender sender;

    /**
     * Tells the claimant their application has been received and is waiting for a caseworker's review. Call it
     * when the application reaches awaiting review: when the fee is paid, or on submit for a type with no fee.
     */
    public void notifyClaimantOfApplicationReceived(long caseReference, SelectEnforcementType enforcementType) {
        notifyAfterCommit(caseReference, EnforcementEmail.forApplicationReceived(enforcementType), Map.of());
    }

    /**
     * Tells the claimant the caseworker approved their application, with the warrant or writ number.
     *
     * <p>Only a warrant and a writ of possession have an approval email: the restitution wording is TBC
     * (OPEN-QUESTIONS 51), so nothing is sent for those types.</p>
     */
    public void notifyClaimantOfApproval(long caseReference, SelectEnforcementType enforcementType,
                                         String enforcementNumber) {
        notifyAfterCommit(caseReference, EnforcementEmail.forApproval(enforcementType),
                          Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, nullToEmpty(enforcementNumber)));
    }

    /**
     * Tells the claimant the caseworker rejected their application, with the caseworker's reason.
     *
     * <p>Only a warrant of possession has a rejection email: the writ and restitution wording is TBC
     * (OPEN-QUESTIONS 51), so nothing is sent for those types.</p>
     */
    public void notifyClaimantOfRejection(long caseReference, SelectEnforcementType enforcementType,
                                          String rejectionReason) {
        notifyAfterCommit(caseReference, EnforcementEmail.forRejection(enforcementType),
                          Map.of(EnforcementEmail.REJECTION_REASON, nullToEmpty(rejectionReason)));
    }

    /**
     * Asks the claimant to confirm the eviction date the bailiff has just scheduled.
     */
    public void notifyClaimantOfScheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        notifyAfterCommit(caseReference, EnforcementEmail.EVICTION_SCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Asks the claimant to confirm the new eviction date the bailiff has just rescheduled to.
     */
    public void notifyClaimantOfRescheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        notifyAfterCommit(caseReference, EnforcementEmail.EVICTION_RESCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Tells the claimant they have confirmed they can attend the eviction on this date.
     */
    public void notifyClaimantOfConfirmedAttendance(long caseReference, LocalDateTime evictionDateTime) {
        notifyAfterCommit(caseReference, EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE,
                          evictionPersonalisation(evictionDateTime));
    }

    /**
     * Tells the claimant the court has their answer that they cannot attend the eviction.
     */
    public void notifyClaimantOfDeclinedAttendance(long caseReference) {
        notifyAfterCommit(caseReference, EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE, Map.of());
    }

    /**
     * The date as the confirmation screens show it, so the email and the screen agree: "Thursday 25 June 2026
     * at 10:00am".
     */
    private static Map<String, Object> evictionPersonalisation(LocalDateTime evictionDateTime) {
        String formatted = evictionDateTime == null ? "" : EvictionDateFormat.format(evictionDateTime);
        return Map.of(EnforcementEmail.EVICTION_DATE_TIME, formatted);
    }

    private void notifyAfterCommit(long caseReference, EnforcementEmail email,
                                   Map<String, Object> personalisation) {
        if (email == null) {
            log.info("No enforcement email has been specified for this outcome on case {}", caseReference);
            return;
        }
        AfterCommit.run(() -> {
            try {
                sender.send(caseReference, email, personalisation);
            } catch (RuntimeException ex) {
                log.error("Failed to schedule the enforcement email \"{}\" for case {}", email.getTemplateName(),
                          caseReference, ex);
            }
        });
    }

    // Notify rejects a null personalisation value, so send an empty placeholder rather than failing the email.
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
