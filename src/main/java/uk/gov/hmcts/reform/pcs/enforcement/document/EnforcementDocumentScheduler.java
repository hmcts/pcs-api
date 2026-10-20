package uk.gov.hmcts.reform.pcs.enforcement.document;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The tasks are written in the caller's transaction, so an event cannot commit without queueing its documents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnforcementDocumentScheduler {

    private final SchedulerClient schedulerClient;

    public void scheduleApplicationDocuments(UUID enforcementId, SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            log.warn("Enforcement {} has no enforcement type, so no application documents were generated",
                     enforcementId);
            return;
        }
        scheduleAll(enforcementId, EnforcementOutput.forApplication(enforcementType));
    }

    public void scheduleApprovalDocument(UUID enforcementId, SelectEnforcementType enforcementType) {
        if (enforcementType == null) {
            log.warn("Enforcement {} has no enforcement type, so no warrant or writ was generated",
                     enforcementId);
            return;
        }
        scheduleAll(enforcementId, List.of(EnforcementOutput.forApproval(enforcementType)));
    }

    public void scheduleEvictionNotices(UUID enforcementId) {
        scheduleAll(enforcementId, EnforcementOutput.forEvictionSchedule());
    }

    private void scheduleAll(UUID enforcementId, List<EnforcementOutput> outputs) {
        outputs.forEach(output -> schedule(enforcementId, output));
    }

    /**
     * Uses the real clock, not the injected one: db-scheduler judges due time against the system clock.
     */
    private void schedule(UUID enforcementId, EnforcementOutput output) {
        schedulerClient.scheduleIfNotExists(
            EnforcementDocumentGenerationComponent.ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR
                .instance(instanceId(enforcementId, output))
                .data(EnforcementDocumentTaskData.builder()
                          .enforcementId(enforcementId)
                          .output(output)
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    static String instanceId(UUID enforcementId, EnforcementOutput output) {
        String instanceId = enforcementId + "-" + output.name();
        return output.isRepeatable() ? instanceId + "-" + UUID.randomUUID() : instanceId;
    }
}
