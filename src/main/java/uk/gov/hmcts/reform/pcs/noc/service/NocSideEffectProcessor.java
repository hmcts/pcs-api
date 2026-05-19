package uk.gov.hmcts.reform.pcs.noc.service;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobStatus;
import uk.gov.hmcts.reform.pcs.noc.repository.NocSideEffectJobRepository;

@Service
@Slf4j
@AllArgsConstructor
public class NocSideEffectProcessor {

    private final NocSideEffectJobRepository repository;
    private final PcsCaseUserAccessService caseUserAccessService;

    @Transactional
    public boolean process(UUID jobId) {
        NocSideEffectJobEntity job = repository.findById(jobId).orElse(null);

        if (job == null || job.getStatus() == NocSideEffectJobStatus.COMPLETED) {
            return true;
        }

        job.setStatus(NocSideEffectJobStatus.IN_PROGRESS);
        job.setAttempts(job.getAttempts() + 1);

        try {
            processJob(job);
            job.setStatus(NocSideEffectJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setLastError(null);
            return true;
        } catch (RuntimeException ex) {
            job.setStatus(NocSideEffectJobStatus.FAILED);
            job.setLastError(ex.getMessage());
            job.setAvailableAt(LocalDateTime.now().plusMinutes(5));
            log.warn("NoC side-effect job {} failed on attempt {}", job.getId(), job.getAttempts(), ex);
            return false;
        }
    }

    private void processJob(NocSideEffectJobEntity job) {
        switch (job.getType()) {
            case GRANT_CASE_ROLE -> caseUserAccessService.grantCaseRole(job);
            case REVOKE_CASE_ROLE -> caseUserAccessService.revokeCaseRole(job);
            case NOTIFY_CASE_ACCESS_REMOVED -> log.info(
                "Queued NoC case access removed notification for user {} and case {}",
                job.getUserId(),
                job.getCaseReference()
            );
            case NOTIFY_PARTY_REPRESENTATION_RETAINED -> log.info(
                "Queued NoC retained case access notification for user {} and case {}",
                job.getUserId(),
                job.getCaseReference()
            );
            case AUDIT -> log.info("NoC audit side-effect for case {}: {}", job.getCaseReference(), job.getDetail());
            default -> throw new IllegalStateException("Unsupported NoC side-effect type " + job.getType());
        }
    }
}
