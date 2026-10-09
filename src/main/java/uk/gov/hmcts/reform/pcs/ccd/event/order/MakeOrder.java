package uk.gov.hmcts.reform.pcs.ccd.event.order;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderSteps;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;

/**
 * A judge makes an order in pcs-frontend. Each judge has one working draft per case, which a change
 * with no id starts. An order a caseworker returned is the judge's to change and resubmit, opened from
 * the task its return gave them.
 */
@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<OrderStart, MakeOrderRequest> MAKE_ORDER =
        ExternalEventId.of("ext:makeOrder", OrderStart.class, MakeOrderRequest.class);

    /** The states that leave an order its author's to change. */
    private static final Set<OrderState> CHANGEABLE = Set.of(DRAFT, RETURNED_TO_JUDGE);
    private static final String NO_LONGER_CHANGEABLE = "The order is no longer waiting for you to change it";

    private final OrderRepository orderRepository;
    private final OrderSteps orderSteps;
    private final PcsCaseRepository pcsCaseRepository;
    private final OrderCaseContext orderCaseContext;
    private final ClientContextRetriever clientContextRetriever;
    private final SecurityContextService securityContextService;
    private final OrderTasks orderTasks;
    private final TaskCompletionService taskCompletionService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(MAKE_ORDER, this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Make an order")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD,
                UserRole.JUDGE,
                UserRole.FEE_PAID_JUDGE,
                UserRole.CIRCUIT_JUDGE,
                UserRole.LEADERSHIP_JUDGE)
            // Caseworkers review the orders judges submit, so they see them in the case history.
            .grant(Set.of(Permission.R), CASEWORKER_ROLES)
            .onStart(this::start);
    }

    private ExternalStartResponse<OrderStart> start(ExternalStartRequest start) {
        long caseReference = start.caseReference();
        UUID judge = UUID.fromString(start.user().id());
        OrderStart.Order workingOrder = clientContextRetriever.getOrderId()
            .map(orderId -> chosenOrder(orderId, caseReference, judge))
            .orElseGet(() -> orderRepository.findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
                    caseReference, judge, DRAFT)
                .map(draft -> OrderStart.Order.of(draft, orderSteps.latest(draft), null))
                .orElse(OrderStart.Order.NONE));
        return ExternalStartResponse.started(
            new OrderStart(workingOrder, orderCaseContext.of(findCase(caseReference))));
    }

    /** A returned order the judge's task links to, with its query. */
    private OrderStart.Order chosenOrder(UUID orderId, long caseReference, UUID judge) {
        OrderEntity order = orderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(orderId, caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because(NO_LONGER_CHANGEABLE));
        return OrderStart.Order.of(order, orderSteps.latest(order), orderSteps.query(order).orElse(null));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<MakeOrderRequest> submit) {
        MakeOrderRequest request = submit.payload();
        OrderChange change = request.order();
        long caseReference = submit.caseReference();
        UUID judge = UUID.fromString(submit.user().id());
        OrderEntity order = change.id() == null
            ? newDraft(caseReference, judge)
            : workingDraft(caseReference, judge, change);
        boolean resubmitted = order.getState() == RETURNED_TO_JUDGE;
        OrderState to = request.action() == SUBMIT_FOR_REVIEW ? SUBMITTED_FOR_REVIEW : order.getState();
        OrderStepEntity step = orderSteps.append(order, to, judge, OrderStepEntity.builder()
            .orderType(change.orderType())
            .formData(change.formData())
            .docweaveSnapshot(change.docweaveSnapshot()));
        if (request.action() != SUBMIT_FOR_REVIEW) {
            return ExternalSubmitResponse.accepted("Order draft saved", "Saved an order as a draft");
        }
        // Only a returned order comes with a task, the one its return gave the judge.
        submit.afterCommit(taskCompletionService.complete(request.taskId()));
        orderTasks.askStaffToReview(caseReference, step, resubmitted);
        return ExternalSubmitResponse.accepted(
            "Order submitted for review", "Submitted an order for caseworker review");
    }

    /** A judge has one draft per case, which the database enforces. */
    private OrderEntity newDraft(long caseReference, UUID judge) {
        return OrderEntity.builder()
            .pcsCase(findCase(caseReference))
            .authorIdamUserId(judge)
            .authorName(securityContextService.getCurrentUserDetails().getName())
            .state(DRAFT)
            .build();
    }

    private OrderEntity workingDraft(long caseReference, UUID judge, OrderChange change) {
        OrderEntity draft = orderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
                change.id(), caseReference, judge, CHANGEABLE)
            .orElseThrow(() -> ExternalRejection.because("The order draft does not exist for this case"));
        orderSteps.latest(draft, change.version());
        return draft;
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
