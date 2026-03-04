package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworkerverifydefendants.ValidateDefendantList;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ValidateDefendantListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.taskmanagement.TaskManagementService;
import uk.gov.hmcts.reform.pcs.taskmanagement.model.TaskType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_CLAIM_VALIDATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.STRUCK_OUT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerValidateClaim;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;

@Slf4j
@Component
@AllArgsConstructor
public class CaseworkerValidateClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final SchedulerClient schedulerClient;
    private final FeeService feeService;
    private final MoneyFormatter moneyFormatter;
    private final ValidateDefendantListRenderer validateDefendantListRenderer;
    private final TaskManagementService taskManagementService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(caseworkerValidateClaim.name(), this::submit, this::start)
                .forState(AWAITING_CLAIM_VALIDATION)
                .name("Validate pending claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ValidateDefendantList());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        List<ListValue<Party>> allDefendants = caseData.getAllDefendants();

        List<Party> defendants = allDefendants.stream()
            .map(ListValue::getValue)
            .toList();

        caseData.setDefendantListMarkdown(validateDefendantListRenderer.render(defendants));

        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        VerticalYesNo defendantListValid = caseData.getDefendantListValid();

        taskManagementService.enqueueCompletionTasks(List.of(TaskType.CHECK_MULTIPLE_DEFENDANTS), caseReference);

        if (defendantListValid == VerticalYesNo.YES) {
            FeeDetails feeDetails = scheduleCaseIssueFeePayment(caseReference, caseData);

            return SubmitResponse.<State>builder()
                .state(PENDING_CASE_ISSUED)
                .confirmationBody(getClaimApprovedMarkdown(feeDetails.getFeeAmount()))
                .build();
        } else {
            return SubmitResponse.<State>builder()
                .state(STRUCK_OUT)
                .confirmationBody(getClaimRejectedMarkdown())
                .build();
        }
    }


    @SuppressWarnings("DuplicatedCode")
    private FeeDetails scheduleCaseIssueFeePayment(long caseReference, PCSCase caseData) {
        String responsibleParty = caseData.getClaimantInformation().getClaimantName();

        FeeDetails feeDetails = feeService.getFee(FeeType.CASE_ISSUE_FEE);

        String taskId = UUID.randomUUID().toString();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeType(FeeType.CASE_ISSUE_FEE.getCode())
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(String.valueOf(caseReference))
            .responsibleParty(responsibleParty)
            .build();

        schedulerClient.scheduleIfNotExists(
            FEE_CASE_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(taskData)
                .scheduledTo(Instant.now())
        );

        return feeDetails;
    }

    private static String getClaimRejectedMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
              <span class="govuk-panel__title govuk-!-font-size-36">Claim struck out</span>
            </div>

            The claim has failed validation and been struck out.
            """;
    }

    private String getClaimApprovedMarkdown(BigDecimal feeAmount) {
        String caseIssueFee = moneyFormatter.formatFee(feeAmount);

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
              <span class="govuk-panel__title govuk-!-font-size-36">Claim approved</span>
            </div>

            The claim has been approved and a payment request for %s will now be made to the claimant.
            """.formatted(caseIssueFee);
    }

}
