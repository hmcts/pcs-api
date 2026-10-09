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
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderDocumentService;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.MEDIUM_TEXT_LIMIT;

/**
 * A caseworker reviews an order a judge submitted, through pcs-frontend's confirm order review
 * journey, opened from their Work Allocation task. Starting the event sends them the order the task
 * names and the case; submitting either
 * returns the order to the judge with a query, or issues the order as the caseworker has it, saying
 * how it is to be issued, and adds any review dates to the case. The caseworker may change the
 * judge's form and wording; the judge's order stays on its earlier steps. The wording is issued as
 * the frontend exports it from Docweave: what was written in the editor is the order. Either
 * completes the caseworker's task, and returning it gives its judge one. Issuing an order generates
 * its document; serving the issued order is not built yet.
 */
@Component
@AllArgsConstructor
public class ConfirmOrderReview implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, ConfirmOrderReviewRequest> CONFIRM_ORDER_REVIEW =
        ExternalEventId.of("ext:confirmOrderReview", OrderStart.class, ConfirmOrderReviewRequest.class);

    private static final String OPEN_FROM_TASK = "Open the order to review from its task";

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

    /** Starts the review of the order the caseworker's task links to. */
    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        UUID orderId = clientContextRetriever.getOrderId(OPEN_FROM_TASK)
            .orElseThrow(() -> ExternalRejection.because(OPEN_FROM_TASK));
        OrderEntity order = orderAwaitingReview(orderId, start.caseReference());
        return ExternalStartResponse.started(new OrderStart(
            OrderStart.Order.of(order, orderSteps.latest(order), null), orderCaseContext.of(order.getPcsCase())));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<ConfirmOrderReviewRequest> submit) {
        ConfirmOrderReviewRequest request = submit.payload();
        long caseReference = submit.caseReference();
        OrderEntity order = orderAwaitingReview(request.orderId(), caseReference);
        OrderStepEntity submitted = orderSteps.latest(order, request.version(), "The order");
        UserInfo caseworker = securityContextService.getCurrentUserDetails();
        UUID caseworkerId = UUID.fromString(caseworker.getUid());
        submit.afterCommit(taskCompletionService.complete(request.taskId()));

        if (request.action() == RETURN_TO_JUDGE) {
            String query = validQuery(request.queryToJudge());
            // The order goes back as the judge submitted it, with the query the judge will read.
            OrderStepEntity returned = orderSteps.append(order, RETURNED_TO_JUDGE, caseworkerId,
                OrderStepEntity.builder()
                    .orderType(submitted.getOrderType())
                    .formData(submitted.getFormData())
                    .docweaveSnapshot(submitted.getDocweaveSnapshot())
                    .note(caseNoteService.addCaseNote(order.getPcsCase(), query)));
            orderTasks.askJudgeToChange(caseReference, returned, query);
            return ExternalSubmitResponse.accepted("Order returned to judge", "Returned an order to the judge");
        }

        Issue issue = request.issue();
        if (!partyIds(order).containsAll(issue.partiesToServe())) {
            throw ExternalRejection.because("The order can only be served on parties to the case");
        }
        order.setSeal(issue.seal());
        order.setFinalOrder(issue.finalOrder());
        order.setServeAllParties(issue.serveAllParties());
        order.setPartiesServed(issue.serveAllParties() ? null : issue.partiesToServe());
        order.setNextStepsComplete(issue.nextStepsComplete());
        orderSteps.append(order, ISSUED, caseworkerId, OrderStepEntity.builder()
            .orderType(issue.order().orderType())
            .formData(issue.order().formData())
            .docweaveSnapshot(issue.order().docweaveSnapshot())
            .orderHtml(issue.order().html()));
        // Scheduled here rather than by the document service, which the event cannot depend on: the
        // service runs system events, whose runtime is built from the events.
        schedulerClient.scheduleIfNotExists(OrderDocumentService.generation(order, caseworker));
        if (!issue.reviewDates().isEmpty()) {
            caseReviewDateService.addCaseReviewDates(caseReference, issue.reviewDates().stream()
                .map(ConfirmOrderReview::toReviewDate)
                .toList());
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

    /** The ids of the parties to the claim, whom the order may be served on. */
    private static Set<String> partyIds(OrderEntity order) {
        return order.getPcsCase().getClaims().stream()
            .flatMap(claim -> claim.getClaimParties().stream())
            .map(claimParty -> claimParty.getId().getPartyId().toString())
            .collect(Collectors.toSet());
    }

    private static ReviewDate toReviewDate(ReviewDateEntry entry) {
        return ReviewDate.builder()
            .date(entry.date())
            .reason(entry.reason())
            .description(entry.description())
            .build();
    }
}
