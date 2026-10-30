package uk.gov.hmcts.reform.pcs.ccd.event.action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@Component
@Slf4j
@AllArgsConstructor
public class ClaimPaymentAction implements PaymentSuccessAction {

    private final PcsCaseService pcsCaseService;
    private final ClaimFormScheduler claimFormScheduler;
    private final DefendantAccessCodeService defendantAccessCodeService;
    private final SchedulerClient schedulerClient;

    @Override
    public EventId eventId() {
        return EventId.claimIssuePayment;
    }

    @Override
    public SystemEventResult apply(PaymentStateTaskData taskData) {
        long caseReference = taskData.getCaseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        if (pcsCaseEntity.getClaims().getFirst().getClaimIssuedDate() == null) {
            log.info("Payment confirmed for case {} - issuing case and scheduling tasks", caseReference);
            pcsCaseService.setCaseIssuedDate(caseReference);
            claimFormScheduler.scheduleClaimFormGeneration(caseReference);
            // Case issued (status -> CASE_ISSUED): generate the defendant access-code letters.
            scheduleAccessCodeFormGeneration(caseReference);
        }
        return stateTransition(taskData, State.CASE_ISSUED);
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
