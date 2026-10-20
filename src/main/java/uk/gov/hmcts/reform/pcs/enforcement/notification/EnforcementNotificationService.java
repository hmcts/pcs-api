package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.util.EvictionDateFormat;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementNotificationService {

    private final EnforcementNotificationSender sender;

    public void notifyClaimantOfApplicationReceived(long caseReference, SelectEnforcementType enforcementType) {
        queue(caseReference, EnforcementEmail.forApplicationReceived(enforcementType), Map.of());
    }

    public void notifyClaimantOfApproval(long caseReference, SelectEnforcementType enforcementType,
                                         String enforcementNumber) {
        queue(caseReference, EnforcementEmail.forApproval(enforcementType),
                          Map.of(EnforcementEmail.ENFORCEMENT_NUMBER, nullToEmpty(enforcementNumber)));
    }

    public void notifyClaimantOfRejection(long caseReference, SelectEnforcementType enforcementType,
                                          String rejectionReason) {
        queue(caseReference, EnforcementEmail.forRejection(enforcementType),
                          Map.of(EnforcementEmail.REJECTION_REASON, nullToEmpty(rejectionReason)));
    }

    public void notifyClaimantOfScheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.EVICTION_SCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    public void notifyClaimantOfRescheduledEviction(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.EVICTION_RESCHEDULED,
                          evictionPersonalisation(evictionDateTime));
    }

    public void notifyClaimantOfConfirmedAttendance(long caseReference, LocalDateTime evictionDateTime) {
        queue(caseReference, EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE,
                          evictionPersonalisation(evictionDateTime));
    }

    public void notifyClaimantOfDeclinedAttendance(long caseReference) {
        queue(caseReference, EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE, Map.of());
    }

    private static Map<String, Object> evictionPersonalisation(LocalDateTime evictionDateTime) {
        String formatted = evictionDateTime == null ? "" : EvictionDateFormat.format(evictionDateTime);
        return Map.of(EnforcementEmail.EVICTION_DATE_TIME, formatted);
    }

    private void queue(long caseReference, EnforcementEmail email, Map<String, Object> personalisation) {
        if (email == null) {
            log.info("No enforcement email has been specified for this outcome on case {}", caseReference);
            return;
        }
        sender.send(caseReference, email, personalisation);
    }

    // Notify rejects a null personalisation value.
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
