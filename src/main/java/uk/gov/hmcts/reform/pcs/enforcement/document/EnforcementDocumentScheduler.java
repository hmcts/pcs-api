package uk.gov.hmcts.reform.pcs.enforcement.document;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.util.AfterCommit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Schedules the enforcement documents an event has to produce, one db-scheduler task per document.
 *
 * <p>Every method is safe to call from an event's submit handler. The tasks are scheduled once the event's
 * transaction has committed, so nothing is generated for data that is rolled back, and nothing here talks to
 * Docmosis, so a Docmosis outage cannot fail the event: the task retries on its own. A failure to schedule is
 * logged rather than thrown, because it happens after the commit and the event has already succeeded.</p>
 *
 * <p>A document an enforcement has only one of is scheduled under an instance id made from the enforcement
 * and the document, so scheduling it twice is a no-op. The notices are generated again on every schedule and
 * reschedule, so each of those gets a new instance id.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnforcementDocumentScheduler {

    private final SchedulerClient schedulerClient;

    /**
     * The application, and for a warrant the risk assessment, once the application has been submitted and paid
     * for.
     */
    public void scheduleApplicationDocuments(UUID enforcementId, SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            log.warn("Enforcement {} has no enforcement type, so no application documents were generated",
                     enforcementId);
            return;
        }
        scheduleAfterCommit(enforcementId, EnforcementOutput.forApplication(enforcementType));
    }

    /**
     * The sealed warrant or writ, once the caseworker has approved the application.
     */
    public void scheduleApprovalDocument(UUID enforcementId, SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            log.warn("Enforcement {} has no enforcement type, so no warrant or writ was generated",
                     enforcementId);
            return;
        }
        scheduleAfterCommit(enforcementId, List.of(EnforcementOutput.forApproval(enforcementType)));
    }

    /**
     * The notice of eviction and notice of appointment, each time the eviction is scheduled or rescheduled.
     */
    public void scheduleEvictionNotices(UUID enforcementId) {
        scheduleAfterCommit(enforcementId, EnforcementOutput.forEvictionSchedule());
    }

    private void scheduleAfterCommit(UUID enforcementId, List<EnforcementOutput> outputs) {
        AfterCommit.run(() -> outputs.forEach(output -> schedule(enforcementId, output)));
    }

    private void schedule(UUID enforcementId, EnforcementOutput output) {
        try {
            schedulerClient.scheduleIfNotExists(
                EnforcementDocumentGenerationComponent.ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR
                    .instance(instanceId(enforcementId, output))
                    .data(EnforcementDocumentTaskData.builder()
                              .enforcementId(enforcementId)
                              .output(output)
                              .build())
                    .scheduledTo(Instant.now())
            );
        } catch (RuntimeException ex) {
            log.error("Failed to schedule the {} for enforcement {}", output.getTitle(), enforcementId, ex);
        }
    }

    static String instanceId(UUID enforcementId, EnforcementOutput output) {
        String instanceId = enforcementId + "-" + output.name();
        return output.isRepeatable() ? instanceId + "-" + UUID.randomUUID() : instanceId;
    }
}
