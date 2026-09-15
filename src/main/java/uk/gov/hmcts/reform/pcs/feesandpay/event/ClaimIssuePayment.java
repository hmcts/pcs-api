package uk.gov.hmcts.reform.pcs.feesandpay.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@Component
@AllArgsConstructor
@Slf4j
public class ClaimIssuePayment implements CCDConfig<PCSCase, State, UserRole> {

    private final SchedulerClient schedulerClient;
    private final PcsCaseService pcsCaseService;
    private final DefendantAccessCodeService defendantAccessCodeService;
    private final ClaimFormScheduler claimFormScheduler;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(claimIssuePayment.name(), this::submit)
            .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
            .name("Payment Confirmation")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.SYSTEM_USER)
            .grant(Permission.R, UserRole.CLAIMANT)
            .grant(Permission.R, UserRole.PCS_SOLICITOR)
            .grant(Permission.R, UserRole.GA_CLAIMANT_SOLICITOR)
            .grant(Permission.R, UserRole.CITIZEN)
            .grant(Permission.R, UserRole.CTSC_ADMIN)
            .grant(Permission.R, UserRole.CTSC_TEAM_LEADER)
            .grant(Permission.R, UserRole.DEFENDANT)
            .grant(Permission.R, UserRole.PCS_CASE_WORKER)
            .grant(Permission.R, UserRole.DEFENDANT_SOLICITOR)
            .grant(Permission.R, UserRole.GA_DEFENDANT_SOLICITOR)
            .grant(Permission.R, UserRole.HEARING_CENTRE_ADMIN)
            .grant(Permission.R, UserRole.HEARING_CENTRE_TEAM_LEADER)
            .grant(Permission.R, UserRole.JUDGE)
            .grant(Permission.R, UserRole.LEADERSHIP_JUDGE)
            .grant(Permission.R, UserRole.WLU_ADMIN)
            .grant(Permission.R, UserRole.WLU_TEAM_LEADER)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();
        if (caseData.getDateIssued() == null) {
            log.info("Payment confirmed for case {} - issuing case and scheduling claim-form, "
                     + "access-code letter generation and claim-issued notification", caseReference);
            pcsCaseService.setCaseIssuedDate(caseReference);
            claimFormScheduler.scheduleClaimFormGeneration(caseReference);
            // Case issued (status -> CASE_ISSUED): generate the defendant access-code letters.
            scheduleAccessCodeFormGeneration(caseReference);
            scheduleClaimIssuedNotification(caseReference);
        }
        return SubmitResponse.<State>builder().state(State.CASE_ISSUED).build();
    }

    private void scheduleClaimIssuedNotification(long caseReference) {
        Optional<Integer> feePaymentId = findClaimIssueFeePaymentId(caseReference);
        if (feePaymentId.isEmpty()) {
            log.warn(
                "No CLAIM fee payment found for case {}; skipping claim-issued email scheduling",
                caseReference
            );
            return;
        }

        String taskId = UUID.randomUUID().toString();
        log.info(
            "Scheduling fee payment paid notification for: {}, with task id: {}",
            feePaymentId.get(),
            taskId
        );

        schedulerClient.scheduleIfNotExists(
            FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR
                .instance(taskId)
                .data(FeePaymentStatusChangeTaskData.builder()
                          .feePaymentId(feePaymentId.get())
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private Optional<Integer> findClaimIssueFeePaymentId(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        if (pcsCaseEntity.getClaims() == null || pcsCaseEntity.getClaims().isEmpty()) {
            return Optional.empty();
        }

        ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();
        if (claimEntity.getFeePayments() == null || claimEntity.getFeePayments().isEmpty()) {
            return Optional.empty();
        }

        return claimEntity.getFeePayments().stream()
            .filter(feePayment -> feePayment.getPaymentCallbackHandlerType() == PaymentCallbackHandlerType.CLAIM)
            .max(Comparator.comparing(FeePaymentEntity::getId, Comparator.nullsLast(Integer::compareTo)))
            .map(FeePaymentEntity::getId);
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
