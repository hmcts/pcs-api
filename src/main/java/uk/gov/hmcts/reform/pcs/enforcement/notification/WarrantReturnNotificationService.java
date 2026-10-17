package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

import java.util.Map;
import java.util.Optional;

/**
 * Emails the claimant when a bailiff records a warrant return (HDPI-8424).
 *
 * <p>Every return name shares one Notify template: the body depends on the return name and is passed as the
 * {@code warrantReturnBody} personalisation, with the return name as {@code returnName}. Nothing is sent
 * until {@code enforcement-notify.templates.warrant-return} is set, and no such template exists in Notify
 * yet.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantReturnNotificationService {

    private final EnforcementNotificationSender sender;

    /**
     * Schedules the claimant's email for the return, if the return name sends one and the template is
     * configured. Returns whether an email was scheduled.
     *
     * <p>TODO HDPI-8424: "Warrant withdrawn" must also notify each defendant, by their contact preference
     * (email or post), once the notice of eviction has been served on them. The content is TBC, and serving
     * the notice (HDPI-7346) is not modelled on this branch.</p>
     *
     * <p>The send runs in a transaction of its own, so a failure to schedule the email cannot undo the
     * recorded return. Call it only once the return has committed — {@code RecordWarrantReturn} defers it
     * with {@link uk.gov.hmcts.reform.pcs.enforcement.util.AfterCommit} — because that transaction commits
     * independently of the caller's, and the claimant must not be told about a return that is then rolled
     * back.</p>
     */
    public boolean notifyClaimant(long caseReference, WarrantReturnName returnName, String additionalDetails) {
        Optional<String> body = WarrantReturnClaimantEmails.body(returnName, additionalDetails);
        if (body.isEmpty()) {
            log.info("Warrant return {} on case {} does not notify the claimant", returnName, caseReference);
            return false;
        }
        return sender.send(caseReference, EnforcementEmail.WARRANT_RETURN, Map.of(
            EnforcementEmail.RETURN_NAME, returnName.getLabel(),
            EnforcementEmail.WARRANT_RETURN_BODY, body.get()));
    }
}
