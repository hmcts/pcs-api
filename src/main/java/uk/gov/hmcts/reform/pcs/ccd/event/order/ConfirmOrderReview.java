package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalEventId;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitResponse;
import uk.gov.hmcts.reform.pcs.camunda.TaskCompletionService;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.ReviewDateEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderSteps;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.MEDIUM_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.task.OrderDocumentGenerationComponent.ORDER_DOCUMENT_TASK_DESCRIPTOR;

/**
 * A caseworker reviews an order a judge submitted, opened from their Work Allocation task, and either
 * returns it to the judge with a query or issues it, with any changes they made. Issuing generates the
 * order's document; serving it is not built yet.
 */
@Component
@AllArgsConstructor
public class ConfirmOrderReview implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, ConfirmOrderReviewRequest> CONFIRM_ORDER_REVIEW =
        ExternalEventId.of("ext:confirmOrderReview", OrderStart.class, ConfirmOrderReviewRequest.class);

    private final OrderRepository orderRepository;
    private final OrderSteps orderSteps;
    private final OrderCaseContext orderCaseContext;
    private final CaseReviewDateService caseReviewDateService;
    private final SchedulerClient schedulerClient;
    private final ClientContextRetriever clientContextRetriever;
    private final SecurityContextService securityContextService;
    private final OrderTasks orderTasks;
    private final TaskCompletionService taskCompletionService;
    private final CaseNoteService caseNoteService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(CONFIRM_ORDER_REVIEW, this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Confirm order review")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            // Judges see in the case history when their orders were returned or issued.
            .grant(Set.of(Permission.R), JUDICIAL_HISTORY_ROLES)
            .onStart(this::start);
    }

    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        UUID orderId = clientContextRetriever.getOrderId()
            .orElseThrow(() -> ExternalRejection.because("Open the order to review from its task"));
        OrderEntity order = orderAwaitingReview(orderId, start.caseReference());
        return ExternalStartResponse.started(new OrderStart(
            OrderStart.Order.of(order, orderSteps.latest(order), null), orderCaseContext.of(order.getPcsCase())));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<ConfirmOrderReviewRequest> submit) {
        ConfirmOrderReviewRequest request = submit.payload();
        OrderEntity order = orderAwaitingReview(request.orderId(), submit.caseReference());
        OrderStepEntity submitted = orderSteps.latest(order, request.version());
        submit.afterCommit(taskCompletionService.complete(request.taskId()));
        UserInfo caseworker = securityContextService.getCurrentUserDetails();
        return request.action() == RETURN_TO_JUDGE
            ? returnToJudge(order, submitted, validQuery(request.queryToJudge()), caseworker)
            : issue(order, request.issue(), caseworker);
    }

    /** The order goes back as the judge submitted it, with the query the judge will read. */
    private ExternalSubmitResponse<State> returnToJudge(OrderEntity order, OrderStepEntity submitted, String query,
                                                        UserInfo caseworker) {
        OrderStepEntity returned = orderSteps.append(order, RETURNED_TO_JUDGE, UUID.fromString(caseworker.getUid()),
            OrderStepEntity.builder()
                .orderType(submitted.getOrderType())
                .formData(submitted.getFormData())
                .docweaveSnapshot(submitted.getDocweaveSnapshot())
                .note(caseNoteService.addCaseNote(order.getPcsCase(), query)));
        orderTasks.askJudgeToChange(order.getPcsCase().getCaseReference(), returned, query);
        return ExternalSubmitResponse.accepted("Order returned to judge", "Returned an order to the judge");
    }

    private ExternalSubmitResponse<State> issue(OrderEntity order, Issue issue, UserInfo caseworker) {
        order.setSeal(issue.seal());
        orderSteps.append(order, ISSUED, UUID.fromString(caseworker.getUid()), OrderStepEntity.builder()
            .orderType(issue.order().orderType())
            .formData(issue.order().formData())
            .docweaveSnapshot(issue.order().docweaveSnapshot())
            .orderHtml(issue.order().html()));
        schedulerClient.scheduleIfNotExists(ORDER_DOCUMENT_TASK_DESCRIPTOR
            .instance(order.getId().toString())
            .data(new OrderDocumentTaskData(order.getId(), caseworker.getUid(), caseworker.getGivenName(),
                caseworker.getFamilyName()))
            .scheduledTo(Instant.now()));
        if (!issue.reviewDates().isEmpty()) {
            caseReviewDateService.addCaseReviewDates(order.getPcsCase().getCaseReference(),
                issue.reviewDates().stream().map(ConfirmOrderReview::toReviewDate).toList());
        }
        return ExternalSubmitResponse.accepted("Order issued", "Reviewed and issued an order");
    }

    private OrderEntity orderAwaitingReview(UUID orderId, long caseReference) {
        return orderRepository
            .findByIdAndPcsCaseCaseReferenceAndState(orderId, caseReference, SUBMITTED_FOR_REVIEW)
            .orElseThrow(() -> ExternalRejection.because("The order is no longer waiting for review"));
    }

    private static String validQuery(String query) {
        if (query == null || query.isBlank()) {
            throw ExternalRejection.because("Enter a query for the judge");
        }
        if (query.length() > MEDIUM_TEXT_LIMIT) {
            throw ExternalRejection.because("The query must be 500 characters or fewer");
        }
        return query;
    }

    private static ReviewDate toReviewDate(ReviewDateEntry entry) {
        return ReviewDate.builder()
            .date(entry.date())
            .reason(entry.reason())
            .description(entry.description())
            .build();
    }
}
