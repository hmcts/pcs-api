package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.taskmanagement.TaskManagementService;
import uk.gov.hmcts.reform.pcs.taskmanagement.model.TaskType;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_CLAIM_VALIDATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.REQUESTED_FOR_DELETION;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.withdrawClaim;

@Slf4j
@Component
@AllArgsConstructor
public class WithdrawClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final TaskManagementService taskManagementService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(withdrawClaim.name(), this::submit)
            .forStates(AWAITING_CLAIM_VALIDATION, PENDING_CASE_ISSUED)
            .name("Withdraw Claim")
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        taskManagementService.enqueueCancellationTasks(List.of(TaskType.CHECK_MULTIPLE_DEFENDANTS), caseReference);

        return SubmitResponse.<State>builder()
            .state(REQUESTED_FOR_DELETION)
            .confirmationBody(getDeletionMarkdown())
            .build();
    }


    private String getDeletionMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
              <span class="govuk-panel__title govuk-!-font-size-36">Claim withdrawal requested</span>
            </div>

            The claim has been withdrawn.
            """;
    }

}
