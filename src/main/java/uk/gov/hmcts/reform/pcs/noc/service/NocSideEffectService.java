package uk.gov.hmcts.reform.pcs.noc.service;

import static uk.gov.hmcts.reform.pcs.noc.task.NocSideEffectTaskComponent.NOC_SIDE_EFFECT_TASK;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobStatus;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobType;
import uk.gov.hmcts.reform.pcs.noc.model.NocSideEffectTaskData;
import uk.gov.hmcts.reform.pcs.noc.repository.NocSideEffectJobRepository;

@Service
@AllArgsConstructor
public class NocSideEffectService {

    private final NocSideEffectJobRepository repository;
    private final SchedulerClient schedulerClient;

    public void enqueue(NocSideEffectJobEntity job) {
        if (repository.existsByIdempotencyKey(job.getIdempotencyKey())) {
            return;
        }

        NocSideEffectJobEntity saved = repository.save(job);
        schedulerClient.scheduleIfNotExists(
            NOC_SIDE_EFFECT_TASK
                .instance(saved.getIdempotencyKey())
                .data(new NocSideEffectTaskData(saved.getId()))
                .scheduledTo(Instant.now())
        );
    }

    public NocSideEffectJobEntity job(
        long caseReference,
        java.util.UUID partyId,
        NocSideEffectJobType type,
        String userId,
        String organisationId,
        String caseRole,
        String email,
        String detail,
        String idempotencyKey
    ) {
        LocalDateTime now = LocalDateTime.now();
        return NocSideEffectJobEntity.builder()
            .caseReference(caseReference)
            .partyId(partyId)
            .type(type)
            .status(NocSideEffectJobStatus.PENDING)
            .userId(userId)
            .organisationId(organisationId)
            .caseRole(caseRole)
            .email(email)
            .detail(detail)
            .idempotencyKey(idempotencyKey)
            .createdAt(now)
            .availableAt(now)
            .build();
    }
}
