package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

/**
 * Issues a case once its claim fee is paid: stamps the issue date, schedules claim-form generation
 * and schedules the defendant access-code letters. Shared by the claimIssuePayment event handler
 * and the payment callback's system event.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CaseIssueService {

    private final PcsCaseService pcsCaseService;
    private final ClaimService claimService;
    private final ClaimFormScheduler claimFormScheduler;
    private final DefendantAccessCodeService defendantAccessCodeService;
    private final SchedulerClient schedulerClient;

    public void issueCaseIfNotIssued(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        if (claim.getClaimIssuedDate() != null) {
            log.info("Case {} already issued - nothing to do", caseReference);
            return;
        }

        log.info("Payment confirmed for case {} - issuing case and scheduling claim-form and "
                     + "access-code letter generation", caseReference);
        claimService.setClaimIssuedDate(claim);
        claimFormScheduler.scheduleClaimFormGeneration(caseReference);
        scheduleAccessCodeFormGeneration(caseReference);
    }

    // One task per defendant (instance = caseRef:partyId), so each defendant generates and retries
    // independently and scheduleIfNotExists dedupes per defendant - a re-fired payment collapses onto
    // the same instances instead of scheduling duplicate work.
    private void scheduleAccessCodeFormGeneration(long caseReference) {
        for (UUID defendantPartyId : defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(caseReference)) {
            AccessCodeTaskData taskData = AccessCodeTaskData.builder()
                .caseReference(String.valueOf(caseReference))
                .defendantPartyId(defendantPartyId.toString())
                .build();

            schedulerClient.scheduleIfNotExists(
                ACCESS_CODE_TASK_DESCRIPTOR
                    .instance(caseReference + ":" + defendantPartyId)
                    .data(taskData)
                    .scheduledTo(Instant.now())
            );
        }
    }
}
