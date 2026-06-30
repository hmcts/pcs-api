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
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.time.Instant;
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

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(claimIssuePayment.name(), this::submit)
            .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
            .name("Payment Confirmation")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.SYSTEM_USER)
            .grant(Permission.R, UserRole.PCS_SOLICITOR)
            .grant(Permission.R, UserRole.CITIZEN)
            .grant(Permission.R, UserRole.CTSC_ADMIN)
            .grant(Permission.R, UserRole.CTSC_TEAM_LEADER)
            .grant(Permission.R, UserRole.DEFENDANT)
            .grant(Permission.R, UserRole.PCS_CASE_WORKER)
            .grant(Permission.R, UserRole.DEFENDANT_SOLICITOR)
            .grant(Permission.R, UserRole.HEARING_CENTRE_ADMIN)
            .grant(Permission.R, UserRole.HEARING_CENTRE_TEAM_LEADER)
            .grant(Permission.R, UserRole.JUDGE)
            .grant(Permission.R, UserRole.LEADERSHIP_JUDGE)
            .grant(Permission.R, UserRole.WLU_ADMIN)
            .grant(Permission.R, UserRole.WLU_TEAM_LEADER)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Received: {}", eventPayload);
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();
        if (caseData.getDateIssued() == null) {
            log.info("Payment confirmed for case {} - issuing case and scheduling access-code letter generation",
                     caseReference);
            pcsCaseService.setCaseIssuedDate(caseReference);
            // Case issued (status -> CASE_ISSUED): generate the defendant access code access-code letters.
            scheduleAccessCodeFormGeneration(caseReference);
        }
        return SubmitResponse.<State>builder().state(State.CASE_ISSUED).build();
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
